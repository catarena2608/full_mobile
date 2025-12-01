const express = require("express");
const router = express.Router();
const multer = require("multer");
const axios = require("axios");
const FormData = require("form-data");
const Post = require("../models/postModel");
const CountPost = require("../models/countPostModel");
const Like = require("../models/likeModel");
const Save = require("../models/saveModel");
const {
  v4: uuidv4
} = require("uuid");
const {
  getChannel
} = require("../config/rabbitmq");

// 🔍 SEARCH posts theo caption hoặc location
router.get("/search", async (req, res) => {
  try {
    const { q, after } = req.query;
    const limit = 10;

    const query = {};

    if (after) {
      query.createdAt = { $lt: new Date(after) };
    }

    if (q && q.trim() !== "") {
      query.$or = [
        { caption: { $regex: q, $options: "i" } },
        { "location.name": { $regex: q, $options: "i" } }
      ];
    }

    const posts = await Post.find(query)
      .sort({ createdAt: -1 })
      .limit(limit)
      .lean();

    const nextCursor =
      posts.length > 0 ? posts[posts.length - 1].createdAt : null;

    // ==========================
    // 🔥 PUSH STATS PAYLOAD
    // ==========================
    if (q && q.trim() !== "") {
      const regex = new RegExp(q, "ig");
      const targetSet = new Set();

      for (const post of posts) {
        // Extract from caption
        if (post.caption) {
          const capMatches = post.caption.match(regex);
          if (capMatches) capMatches.forEach(m => targetSet.add(m));
        }

        // Extract from location.name
        if (post.location?.name) {
          const locMatches = post.location.name.match(regex);
          if (locMatches) locMatches.forEach(m => targetSet.add(m));
        }
      }

      const channel = getChannel();
      if (channel) {
        const payload = {
          keyword: q,
          type: ["caption", "location"],
          target: [...targetSet]
        };

        console.log("📤 Stats SEARCH payload:", payload);

        channel.sendToQueue(
          process.env.RABBITMQ_STATS_QUEUE,
          Buffer.from(JSON.stringify(payload)),
          { persistent: true }
        );
      }
    }

    return res.json({
      success: true,
      posts,
      nextCursor
    });

  } catch (err) {
    console.error("❌ Lỗi search:", err);
    return res.status(500).json({ success: false, message: err.message });
  }
});

// 🔍 SEARCH posts theo tag
router.get("/tag", async (req, res) => {
  try {
    const { q, after } = req.query;
    const limit = 10;

    const query = {};

    if (after) {
      query.createdAt = { $lt: new Date(after) };
    }

    if (q && q.trim() !== "") {
      query.tag = { $regex: q, $options: "i" };
    }

    const posts = await Post.find(query)
      .sort({ createdAt: -1 })
      .limit(limit)
      .lean();

    const nextCursor =
      posts.length > 0 ? posts[posts.length - 1].createdAt : null;

    // ==========================
    // 🔥 PUSH STATS PAYLOAD
    // ==========================
    if (q && q.trim() !== "") {
      const normalized = q.toLowerCase();
      const target = [];

      for (const post of posts) {
        if (Array.isArray(post.tag)) {
          post.tag.forEach(t => {
            if (t.toLowerCase().includes(normalized)) {
              target.push(t);
            }
          });
        }
      }

      const channel = getChannel();
      if (channel) {
        const payload = {
          keyword: q,
          type: ["tag"],
          target
        };

        console.log("📤 Stats TAG payload:", payload);

        channel.sendToQueue(
          process.env.RABBITMQ_STATS_QUEUE,
          Buffer.from(JSON.stringify(payload)),
          { persistent: true }
        );
      }
    }

    return res.json({
      success: true,
      posts,
      nextCursor
    });

  } catch (err) {
    console.error("❌ Lỗi search tag:", err);
    return res.status(500).json({ success: false, message: err.message });
  }
});

//  Lấy ra các bài viết đã lưu của mình
router.get("/saved", async (req, res) => {
  try {
    const userID = req.headers["x-user-id"];
    if (!userID) return res.status(400).json({
      success: false,
      message: "Thiếu userID trong header"
    });

    const saves = await Save.find({
      userID
    }).lean();
    const postIDs = saves.map((s) => s.postID);

    const posts = await Post.find({
        _id: {
          $in: postIDs
        }
      })
      .sort({
        createdAt: -1
      })
      .lean();

    res.json({
      success: true,
      total: posts.length,
      posts,
    });

  } catch (err) {
    console.error("❌ Lỗi lấy saved:", err);
    res.status(500).json({
      success: false,
      message: err.message
    });
  }
});


// 🧩 GET posts với filter + pagination
router.get("/", async (req, res) => {
  try {
    const userID = req.headers["x-user-id"];
    const {
      after,
      type,
      year,
      month
    } = req.query;

    const limit = 10;

    if (!userID) {
      return res.status(400).json({
        success: false,
        message: "Thiếu userID"
      });
    }

    // -------------------------------
    // 🟦 Build query object
    // -------------------------------
    const query = {};

    // Cursor pagination
    if (after) {
      query.createdAt = {
        $lt: new Date(after)
      };
    }

    // Lọc theo type
    if (type) {
      query.type = type;
    }

    // Lọc theo tháng
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
      .sort({
        createdAt: -1
      })
      .limit(limit)
      .lean();

    if (posts.length === 0) {
      return res.json({
        success: true,
        posts: [],
        nextCursor: null,
      });
    }

    // -------------------------------
    // 🟦 Check meLike nhanh bằng composite ID
    // -------------------------------
    const likeIDs = posts.map((p) => `${userID}_${p._id}`);

    const liked = await Like.find({
      _id: {
        $in: likeIDs
      }
    }).select("_id");
    const likedSet = new Set(liked.map((l) => l._id));

    // 🟦 Check meSave nhanh bằng composite ID
    const saveIDs = posts.map((p) => `${userID}_${p._id}`);

    const saved = await Save.find({
      _id: {
        $in: saveIDs
      }
    }).select("_id");
    const savedSet = new Set(saved.map((s) => s._id));
    const resultPosts = posts.map((p) => ({
      ...p,
      meLike: likedSet.has(`${userID}_${p._id}`),
      meSave: savedSet.has(`${userID}_${p._id}`),
    }));

    const nextCursor = posts[posts.length - 1].createdAt;

    res.json({
      success: true,
      nextCursor,
      posts: resultPosts,
    });

  } catch (err) {
    console.error("❌ Error fetching posts:", err);
    res.status(500).json({
      success: false,
      message: err.message
    });
  }
});
router.get("/:userID", async (req, res) => {
  try {
    const {
      userID
    } = req.params;
    const requestUserID = req.headers["x-user-id"];

    if (!userID) {
      return res.status(400).json({
        success: false,
        message: "Thiếu userID"
      });
    }

    const posts = await Post.find({
      userID
    }).sort({
      createdAt: -1
    }).lean();

    // Check meLike, meSave
    const likeIDs = posts.map((p) => `${requestUserID}_${p._id}`);
    const liked = await Like.find({
      _id: {
        $in: likeIDs
      }
    }).select("_id");
    const likedSet = new Set(liked.map((l) => l._id));

    const saveIDs = posts.map((p) => `${requestUserID}_${p._id}`);
    const saved = await Save.find({
      _id: {
        $in: saveIDs
      }
    }).select("_id");
    const savedSet = new Set(saved.map((s) => s._id));

    const resultPosts = posts.map((p) => ({
      ...p,
      meLike: likedSet.has(`${requestUserID}_${p._id}`),
      meSave: savedSet.has(`${requestUserID}_${p._id}`),
    }));

    res.json({
      success: true,
      total: posts.length,
      posts: resultPosts,
    });

  } catch (err) {
    console.error("❌ Lỗi khi lấy bài viết theo user:", err);
    res.status(500).json({
      success: false,
      message: err.message
    });
  }
});
// ✏️ PATCH sửa bài viết
router.patch("/:postID", async (req, res) => {
  try {
    const {
      postID
    } = req.params;
    const requestUserID = req.headers["x-user-id"];

    // 1. Check owner
    const post = await Post.findById(postID);
    if (!post) return res.status(404).json({
      success: false,
      message: "Không tìm thấy post"
    });

    if (post.userID !== requestUserID) {
      return res.status(403).json({
        success: false,
        message: "Không có quyền sửa bài của người khác"
      });
    }

    // 2. Update
    const {
      caption,
      tag,
      location
    } = req.body;
    const updateData = {};

    if (caption !== undefined) updateData.caption = caption;

    if (tag !== undefined) {
      if (!Array.isArray(tag)) {
        return res.status(400).json({
          success: false,
          message: "tag phải là array"
        });
      }
      if (!tag.includes("edited")) tag.push("edited");
      updateData.tag = tag;
    }

    if (location !== undefined) updateData.location = location;

    const updated = await Post.findByIdAndUpdate(postID, updateData, {
      new: true
    });

    res.json({
      success: true,
      message: "Cập nhật thành công",
      post: updated,
    });

  } catch (err) {
    console.error("❌ Lỗi PATCH:", err);
    res.status(500).json({
      success: false,
      message: err.message
    });
  }
});
// ⚙️ Cấu hình multer (lưu file tạm trong RAM)
const storage = multer.memoryStorage();
const upload = multer({
  storage
});

// 🧩 Upload bài viết
router.post("/upload", upload.array("media", 10), async (req, res) => {
  try {
    const requestUserID = req.headers["x-user-id"];
    const {
      type,
      caption,
      tag,
      location
    } = req.body;

    if (!requestUserID || !type)
      return res.status(400).json({
        success: false,
        message: "Thiếu userID trong header hoặc thiếu type"
      });

    // 🖼 Upload tất cả ảnh lên Cloudinary
    const uploadedUrls = [];
    if (req.files && req.files.length > 0) {
      for (const file of req.files) {
        const formData = new FormData();
        formData.append("file", file.buffer, file.originalname);
        formData.append("upload_preset", "uploadDemo"); // preset Cloudinary của bạn

        const cloudRes = await axios.post(
          "https://api.cloudinary.com/v1_1/dx6uxiydg/image/upload",
          formData, {
            headers: formData.getHeaders()
          }
        );

        uploadedUrls.push(cloudRes.data.secure_url);
      }
    }

    // 🧩 Parse JSON cho tag và location
    const parsedTag = tag ? JSON.parse(tag) : [];
    const parsedLocation = location ? JSON.parse(location) : {
      type: "Point",
      coordinates: [0, 0],
      name: "Không rõ",
    };

    // 🧠 Tạo Post mới
    const newPost = new Post({
      _id: uuidv4(),
      userID: requestUserID,
      type,
      caption,
      tag: parsedTag,
      location: parsedLocation,
      media: uploadedUrls, // ảnh sau khi up Cloudinary
      like: 0,
    });

    await newPost.save();

    // 📊 Cập nhật thống kê số bài viết theo tháng
  try {
      const createdAt = newPost.createdAt || new Date();
      const month = createdAt.getMonth() + 1; // 1-12
      const year = createdAt.getFullYear();

    // Tìm record tháng đó, nếu không có thì tạo
  const updatedCount = await CountPost.findOneAndUpdate(
    { month, year },
    { $inc: { count: 1 } },
    { upsert: true, new: true }
  );

  console.log("📊 Updated countPost:", updatedCount);
} catch (err) {
  console.error("❌ Lỗi cập nhật countPost:", err);
}

    // 🔔 Push event vào với queue notify
    const channel = getChannel();
    if (channel) {
      const payload = {
        actorId: requestUserID,
        type: "new_post",
        targetId: newPost._id,
        userID: [] // Rỗng để Notify Service tự kiếm
      };
      console.log("📤 Sending UNLIKE event to RabbitMQ:", payload);
      channel.sendToQueue(process.env.RABBITMQ_NOTIFY_QUEUE, Buffer.from(JSON.stringify(payload)), {
        persistent: true
      });
    } else {
      console.error("❌ Không thể gửi RabbitMQ: Channel chưa có!");
    }


    res.json({
      success: true,
      message: "Đăng bài thành công 🎉",
      post: newPost,
    });
  } catch (err) {
    console.error("❌ Lỗi upload bài:", err.message);
    res.status(500).json({
      success: false,
      message: "Lỗi server khi upload bài",
      error: err.message,
    });
  }
});

module.exports = router;