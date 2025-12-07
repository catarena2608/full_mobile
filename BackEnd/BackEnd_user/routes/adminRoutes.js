const express = require("express");
const router = express.Router();
const Follow = require("../models/followModel");
const CountFollow = require("../models/countFollowModel");
const CountUser = require("../models/countUserModel");
const User = require("../models/userModel");

/* ==========================================================
   Helper: Tạo range ngày theo day / month / year
========================================================== */
function buildDateRange({ day, month, year }) {
  const now = new Date();
  const y = year ? Number(year) : now.getFullYear();
  const m = month ? Number(month) - 1 : now.getMonth();

  let start, end;

  if (day) {
    start = new Date(y, m, Number(day));
    end   = new Date(y, m, Number(day) + 1);
  } else if (month) {
    start = new Date(y, m, 1);
    end   = new Date(y, m + 1, 1);
  } else if (year) {
    start = new Date(y, 0, 1);
    end   = new Date(y + 1, 0, 1);
  }

  if (start && end) return { start, end };
  return null;
}

/* ==========================================================
   GET /admin/follow
========================================================== */
router.get("/countFollow", async (req, res) => {
  try {
    const { userID, day, month, year } = req.query;
    const range = buildDateRange({ day, month, year });

    /* CASE 1: Có userID → thống kê theo user */
    if (userID) {
      const filter = { to: userID };
      if (range) filter.createdAt = { $gte: range.start, $lt: range.end };

      const count = await Follow.countDocuments(filter);

      return res.json({
        success: true,
        scope: "user",
        userID,
        filters: { day, month, year },
        count
      });
    }

    /* CASE 2: Không userID → thống kê theo hệ thống */
    if (day) {
      const count = await Follow.countDocuments({
        createdAt: { $gte: range.start, $lt: range.end }
      });

      return res.json({
        success: true,
        scope: "system",
        source: "Follow (day-level)",
        filters: { day, month, year },
        count
      });
    }

    // Month/year → dùng CountFollow
    const query = {};
    if (month) query.month = Number(month);
    if (year) query.year = Number(year);

    const list = await CountFollow.find(query).sort({ year: 1, month: 1 });
    const total = list.reduce((s, d) => s + d.count, 0);

    return res.json({
      success: true,
      scope: "system",
      source: "countFollow",
      filters: { day, month, year },
      total,
      detail: list
    });

  } catch (err) {
    console.error("❌ Lỗi thống kê follow:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

/* ==========================================================
   GET /admin/user
========================================================== */
router.get("/countUser", async (req, res) => {
  try {
    const { day, month, year } = req.query;
    const range = buildDateRange({ day, month, year });

    if (day) {
      const count = await User.countDocuments({
        createdAt: { $gte: range.start, $lt: range.end }
      });

      return res.json({
        success: true,
        source: "User (day-level)",
        filters: { day, month, year },
        count
      });
    }

    const query = {};
    if (month) query.month = Number(month);
    if (year) query.year = Number(year);

    const list = await CountUser.find(query).sort({ year: 1, month: 1 });
    const total = list.reduce((s, d) => s + d.count, 0);

    return res.json({
      success: true,
      source: "countUser",
      filters: { day, month, year },
      total,
      detail: list
    });

  } catch (err) {
    console.error("❌ Lỗi thống kê user:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

router.get("/:userID", async (req, res) => {
  try {
    const { userID } = req.params;

    if (!userID) {
      return res.status(400).json({
        success: false,
        message: "Thiếu userID"
      });
    }

    const user = await User.findById(userID)
      .select("-password") // ❗ admin không cần xem password
      .lean();

    if (!user) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy user"
      });
    }

    res.json({
      success: true,
      user
    });

  } catch (err) {
    console.error("❌ Lỗi GET /:userID:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

router.get("/", async (req, res) => {
  try {
    const { day, month, year, limit, cursor } = req.query;

    let filter = {};

    // ------------------ TIME FILTER ------------------
    if (day || month || year) {
      let start = new Date();
      let end = new Date();

      if (year) {
        start = new Date(year, 0, 1);
        end = new Date(Number(year) + 1, 0, 1);
      }

      if (year && month) {
        start = new Date(year, Number(month) - 1, 1);
        end = new Date(year, Number(month), 1);
      }

      if (year && month && day) {
        start = new Date(year, Number(month) - 1, Number(day));
        end = new Date(year, Number(month) - 1, Number(day) + 1);
      }

      if (month && !year) {
        const y = new Date().getFullYear();
        start = new Date(y, Number(month) - 1, 1);
        end = new Date(y, Number(month), 1);
      }

      if (day && !month && !year) {
        const now = new Date();
        start = new Date(now.getFullYear(), now.getMonth(), Number(day));
        end = new Date(now.getFullYear(), now.getMonth(), Number(day) + 1);
      }

      filter.createdAt = { $gte: start, $lt: end };
    }

    // ------------------ PAGINATION ------------------
    let queryLimit = Number(limit) || 10;

    // Nếu có cursor → lọc createdAt < cursor
    if (cursor) {
      filter.createdAt = {
        ...(filter.createdAt || {}),
        $lt: new Date(cursor),
      };
    }

    // Tổng số user thỏa filter (không tính cursor)
    const total = await User.countDocuments(filter);

    const users = await User.find(filter)
      .select("_id user_name name avatar email numPosts numFollowed numFollowing createdAt tags")
      .sort({ createdAt: -1 })            // cursor dựa theo sort này
      .limit(queryLimit + 1)              // +1 để check còn nữa không
      .lean();

    // Xác định next cursor
    let nextCursor = null;
    if (users.length > queryLimit) {
      const last = users[queryLimit - 1];
      nextCursor = last.createdAt.toISOString();
      users.splice(queryLimit); // bỏ phần dư
    }

    res.json({
      success: true,
      total,
      returned: users.length,
      nextCursor,
      filters: { day, month, year, limit: queryLimit },
      users
    });

  } catch (err) {
    console.error("❌ Error in GET /userAdmin:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
