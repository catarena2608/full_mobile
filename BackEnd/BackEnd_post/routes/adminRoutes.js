const express = require("express");
const router = express.Router();
const Post = require("../models/postModel");
const CountPost=require("../models/countPostModel");
const Like = require("../models/likeModel");
const CountLike = require("../models/countLikeModel");
//Dành cho admin
// 📊 GET: Thống kê Like
router.get("/like", async (req, res) => {
  try {
    const { postID, day, month, year } = req.query;

    // ====================================================================================
    // CASE 1: KHÔNG có postID → thống kê toàn hệ thống thông qua countLike
    // ====================================================================================
    if (!postID) {
      const query = {};

      if (month) query.month = Number(month);
      if (year) query.year = Number(year);

      const list = await CountLike.find(query).sort({ year: 1, month: 1 });

      const total = list.reduce((sum, doc) => sum + doc.count, 0);

      return res.json({
        success: true,
        scope: "system",
        source: "countLike",
        filters: { day, month, year },
        total,
        detail: list,
      });
    }

    // ====================================================================================
    // CASE 2: Có postID → thống kê like của 1 bài post (dùng bảng Like)
    // ====================================================================================

    let filter = { postID };

    // --------- Nếu có filter thời gian ---------
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

    const count = await Like.countDocuments(filter);

    return res.json({
      success: true,
      scope: "post",
      postID,
      filters: { day, month, year },
      count,
    });

  } catch (err) {
    console.error("❌ Lỗi thống kê like:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});
// 📊 GET: Thống kê số lượng Post
router.get("/post", async (req, res) => {
  try {
    const { day, month, year } = req.query;

    // CASE 1: Không có day → dùng bảng countPost (nhanh hơn)
    if (!day) {
      const query = {};

      if (month) query.month = Number(month);
      if (year) query.year = Number(year);

      const list = await CountPost.find(query).sort({ year: 1, month: 1 });
      const total = list.reduce((sum, doc) => sum + doc.count, 0);

      return res.json({
        success: true,
        scope: "system",
        source: "countPost",
        filters: { day, month, year },
        total,
        detail: list
      });
    }

    // CASE 2: Có day → phải dùng bảng Post real-time
    let start = new Date();
    let end = new Date();

    // Chỉ truyền day → dùng tháng + năm hiện tại
    if (day && !month && !year) {
      const now = new Date();
      start = new Date(now.getFullYear(), now.getMonth(), Number(day));
      end = new Date(now.getFullYear(), now.getMonth(), Number(day) + 1);
    }

    // day + month nhưng không có year → dùng năm hiện tại
    if (day && month && !year) {
      const y = new Date().getFullYear();
      start = new Date(y, Number(month) - 1, Number(day));
      end = new Date(y, Number(month) - 1, Number(day) + 1);
    }

    // day + month + year → chuẩn
    if (day && month && year) {
      start = new Date(year, Number(month) - 1, Number(day));
      end = new Date(year, Number(month) - 1, Number(day) + 1);
    }

    const count = await Post.countDocuments({
      createdAt: { $gte: start, $lt: end }
    });

    return res.json({
      success: true,
      scope: "system",
      source: "Post",
      filters: { day, month, year },
      count
    });

  } catch (err) {
    console.error("❌ Lỗi thống kê post:", err);
    return res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
