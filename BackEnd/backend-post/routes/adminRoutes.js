const express = require("express");
const router = express.Router();
const Post = require("../models/postModel");
const CountPost=require("../models/countPostModel");
const Like = require("../models/likeModel");
const CountLike = require("../models/countLikeModel");
const Comment=require("../models/commentModel");
//Dành cho admin
// 📊 GET: Thống kê Like
router.get("/countLike", async (req, res) => {
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
router.get("/countPost", async (req, res) => {
  try {
    const { day, month, year } = req.query;

    // CASE 1: Không có day → dùng bảng countPost (nhanh hơn)
    if (!day) {
      const query = {}; //Tính cả deleted:true

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

// 📌 GET /admin/post/:postID
router.get("/:postID", async (req, res) => {
  try {
    const { postID } = req.params;

    if (!postID) {
      return res.status(400).json({
        success: false,
        message: "Thiếu postID",
      });
    }

    const post = await Post.findById(postID).lean();

    if (!post) {
      return res.status(404).json({
        success: false,
        message: "Post không tồn tại",
      });
    }

    return res.json({
      success: true,
      post,
    });

  } catch (err) {
    console.error("❌ Admin GET /:postID error:", err);
    return res.status(500).json({ success: false, message: err.message });
  }
});


// 📌 GET /admin/post  → lấy tất cả hoặc theo filter (after, type, year, month)
router.get("/", async (req, res) => {
  try {
    const { after, type, year, month } = req.query;

    const limit = 20; // Admin lấy nhiều hơn user, hoặc tùy bạn chỉnh

    // -------------------------------
    // 🟦 Build query
    // -------------------------------
    const query = { };

    // Cursor pagination
    if (after) {
      query.createdAt = { $lt: new Date(after) };
    }

    // Lọc theo type
    if (type) query.type = type;

    // Lọc theo tháng (nếu muốn lọc từng tháng trong dashboard admin)
    if (year && month) {
      const m = Number(month) - 1;
      const start = new Date(Number(year), m, 1);
      const end = new Date(Number(year), m + 1, 1);

      query.createdAt = {
        ...(query.createdAt || {}),
        $gte: start,
        $lt: end,
      };
    }

    // -------------------------------
    // 🟦 Query DB
    // -------------------------------
    const posts = await Post.find(query)
      .sort({ createdAt: -1 })
      .limit(limit)
      .lean();

    const nextCursor = posts.length > 0 ? posts[posts.length - 1].createdAt : null;

    return res.json({
      success: true,
      nextCursor,
      total: posts.length,
      posts,
    });

  } catch (err) {
    console.error("❌ Admin GET posts error:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

// 📌 Admin GET: Lấy chi tiết 1 comment theo ID
router.get("/comment/:commentID", async (req, res) => {
  try {
    const { commentID } = req.params;

    if (!commentID) {
      return res.status(400).json({
        success: false,
        message: "Thiếu commentID"
      });
    }

    const comment = await Comment.findById(commentID).lean();

    if (!comment) {
      return res.status(404).json({
        success: false,
        message: "Không tìm thấy comment"
      });
    }

    return res.json({
      success: true,
      comment
    });

  } catch (err) {
    console.error("❌ Lỗi khi lấy comment:", err);
    res.status(500).json({
      success: false,
      message: err.message
    });
  }
});
// 🧩 GET /api/post/comment/:postID?after=...
router.get("/comment/byPost/:postID", async (req, res) => {
  try {
    const {
      postID
    } = req.params;
    const {
      after
    } = req.query;
    const limit = 20;

    if (!postID)
      return res.status(400).json({
        success: false,
        message: "Thiếu postID"
      });

    // ✅ Bước 1: lấy 20 comment depth=0 mới nhất (cũ hơn after nếu có)
    const baseQuery = {
      postID,
      depth: 0
    };
    if (after) baseQuery.createdAt = {
      $lt: new Date(after)
    };

    const parents = await Comment.find(baseQuery)
      .sort({
        createdAt: -1
      })
      .limit(limit)
      .lean();

    if (parents.length === 0) {
      return res.json({
        success: true,
        comments: [],
        nextCursor: null,
      });
    }

    // ✅ Bước 2: lấy ID tầng 0
    const parentIDs = parents.map((c) => c._id);

    // ✅ Bước 3: lấy tất cả reply depth=1 có parentID thuộc tầng 0
    const level1 = await Comment.find({
      postID,
      depth: 1,
      parentID: {
        $in: parentIDs
      },
    }).lean();

    // ✅ Bước 4: lấy tất cả reply depth=2 có parentID thuộc tầng 1
    const level1IDs = level1.map((c) => c._id);
    const level2 = await Comment.find({
      postID,
      depth: 2,
      parentID: {
        $in: level1IDs
      },
    }).lean();

    // ✅ Bước 5: Map tra nhanh
    const level1Map = new Map();
    const level2Map = new Map();

    // Map level2 → nhóm theo parentID
    for (const c of level2) {
      if (!level2Map.has(c.parentID)) level2Map.set(c.parentID, []);
      level2Map.get(c.parentID).push(c);
    }

    // Map level1 → gắn replies từ level2
    for (const c of level1) {
      c.replies = level2Map.get(c._id) || [];
      if (!level1Map.has(c.parentID)) level1Map.set(c.parentID, []);
      level1Map.get(c.parentID).push(c);
    }

    // ✅ Bước 6: Gắn replies vào tầng 0
    for (const p of parents) {
      p.replies = level1Map.get(p._id) || [];
    }

    // ✅ Bước 7: nextCursor cho pagination
    const nextCursor = parents[parents.length - 1].createdAt;

    res.json({
      success: true,
      nextCursor,
      comments: parents,
    });
  } catch (err) {
    console.error("❌ Lỗi khi lấy comment:", err);
    res.status(500).json({
      success: false,
      message: err.message
    });
  }
});

module.exports = router;
