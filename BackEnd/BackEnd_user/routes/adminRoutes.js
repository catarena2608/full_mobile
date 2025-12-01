const express = require("express");
const router = express.Router();
const Follow = require("../models/followModel");
const CountFollow = require("../models/countFollowModel");
const CountUser=require("../models/countUserModel")
const User = require("../models/userModel");
//Dành cho admin
/* ---------------------- GET: Thống kê follow ---------------------- */
router.get("/follow", async (req, res) => {
  try {
    const { userID, day, month, year } = req.query;

    // ================================
    // CASE 1: Có userID → thống kê cho 1 user
    // ================================
    if (userID) {
      let filter = { to: userID };

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

      const count = await Follow.countDocuments(filter);

      return res.json({
        success: true,
        scope: "user",
        userID,
        filters: { day, month, year },
        count
      });
    }

    // ================================
    // CASE 2: Không có userID → thống kê hệ thống
    // ================================
    // Nếu lọc theo "day" → buộc phải query Follow vì countFollow không đủ độ phân giải
    if (day) {
      let start, end;
      const now = new Date();

      const y = year ? Number(year) : now.getFullYear();
      const m = month ? Number(month) - 1 : now.getMonth();

      start = new Date(y, m, Number(day));
      end = new Date(y, m, Number(day) + 1);

      const count = await Follow.countDocuments({
        createdAt: { $gte: start, $lt: end }
      });

      return res.json({
        success: true,
        scope: "system",
        source: "Follow (day-level)",
        filters: { day, month: m + 1, year: y },
        count
      });
    }

    // Trường hợp lọc theo month/year → dùng countFollow
    const query = {};
    if (month) query.month = Number(month);
    if (year) query.year = Number(year);

    // Nếu không có gì → trả toàn bộ hệ thống
    const list = await CountFollow.find(query).sort({ year: 1, month: 1 });

    const total = list.reduce((sum, doc) => sum + doc.count, 0);

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
/* ---------------------- GET: Thống kê user đăng ký ---------------------- */
// GET /user?day=..&month=..&year=..
router.get("/user", async (req, res) => {
  try {
    const { day, month, year } = req.query;

    // ================================
    // CASE 1: Có lọc theo "day" → query trực tiếp User
    // ================================
    if (day) {
      const now = new Date();

      const y = year ? Number(year) : now.getFullYear();
      const m = month ? Number(month) - 1 : now.getMonth();

      const start = new Date(y, m, Number(day));
      const end = new Date(y, m, Number(day) + 1);

      const count = await User.countDocuments({
        createdAt: { $gte: start, $lt: end }
      });

      return res.json({
        success: true,
        scope: "system",
        source: "User (day-level)",
        filters: { day, month: m + 1, year: y },
        count
      });
    }

    // ================================
    // CASE 2: Lọc theo month/year → dùng countUser
    // ================================
    const query = {};
    if (month) query.month = Number(month);
    if (year) query.year = Number(year);

    const list = await CountUser.find(query).sort({ year: 1, month: 1 });

    const total = list.reduce((sum, doc) => sum + doc.count, 0);

    return res.json({
      success: true,
      scope: "system",
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


module.exports = router;
