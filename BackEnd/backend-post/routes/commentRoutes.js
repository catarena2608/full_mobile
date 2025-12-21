const express = require("express");
const router = express.Router();
const Post = require("../models/postModel");
const Comment = require("../models/commentModel"); // model comment
const { getChannel } = require("../config/rabbitmq");

// 🧩 GET /api/post/comment/:postID?after=...
router.get("/:postID", async (req, res) => {
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
// 🧩 POST /api/post/comment
router.post("/", async (req, res) => {
  try {
    const userID = req.header("x-user-id"); // 🎯 Lấy userID từ header
    // 🔥 Check violation (ban post)
    const { checkViolation } = require("../utils/checkViolation");
    const result = await checkViolation(userID, "violation_comment");

    if (!result.expired) {
      return res.status(403).json({
        success: false,
        message: "Bạn đang bị cấm đăng bài",
        expireAt: result.expireAt
      });
    }

    const { postID, content, reply } = req.body;

    if (!userID || !postID || !content) {
      return res.status(400).json({
        success: false,
        message: "Thiếu userID (header), postID hoặc content",
      });
    }

    const post = await Post.findById(postID);
    if (!post) {
      return res.status(404).json({ success: false, message: "Không tìm thấy bài viết" });
    }

    let parentID = null;
    let depth = 0;
    let replyComment = null;

    if (reply) {
      replyComment = await Comment.findById(reply);
      if (!replyComment) {
        return res.status(404).json({
          success: false,
          message: "Không tìm thấy comment gốc để reply",
        });
      }
      depth = replyComment.depth + 1 > 2 ? 2 : replyComment.depth + 1;
      parentID = replyComment._id;
    }

    // Tạo comment mới
    const newComment = await Comment.create({
      postID,
      userID,
      content,
      parentID,
      depth,
    });

    // Tăng số bình luận trong Post
    await Post.findByIdAndUpdate(postID, { $inc: { comment: 1 } });

    // 🔔 Push event vào RabbitMQ
    const channel = getChannel();
    if (!channel) {
      console.error("❌ Không thể gửi RabbitMQ: Channel chưa có!");
    } else {
      const QUEUE = process.env.RABBITMQ_NOTIFY_QUEUE || "notification_queue";

      // 1️⃣ Payload comment → gửi cho chủ bài viết
      const commentPayload = {
        actorId: userID,
        type: "comment",
        targetId: postID,
        userID: post.userID
      };
      console.log("📤 Sending COMMENT event to RabbitMQ:", commentPayload);
      channel.sendToQueue(QUEUE, Buffer.from(JSON.stringify(commentPayload)), { persistent: true });

      // 2️⃣ Nếu là reply → gửi thêm payload reply cho chủ comment được reply
      if (reply && replyComment) {
        const replyPayload = {
          actorId: userID,
          type: "reply",
          targetId: newComment.parentID, // target là comment được reply
          userID: replyComment.userID
        };
        console.log("📤 Sending REPLY event to RabbitMQ:", replyPayload);
        channel.sendToQueue(QUEUE, Buffer.from(JSON.stringify(replyPayload)), { persistent: true });
      }
    }

    res.json({
      success: true,
      message: "Đã thêm bình luận 💬",
      comment: newComment,
    });
  } catch (err) {
    console.error("❌ Lỗi khi thêm bình luận:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

// 🗑 DELETE /api/post/comment/:id
router.delete("/:id", async (req, res) => {
  try {
    const {
      id
    } = req.params;
    const comment = await Comment.findById(id);

    if (!comment)
      return res
        .status(404)
        .json({
          success: false,
          message: "Không tìm thấy bình luận để xóa"
        });

    // ✅ Xóa chính comment này
    await Comment.findByIdAndDelete(id);

    // ✅ Xóa luôn các reply con (nếu có)
    await Comment.deleteMany({
      parentID: id
    });

    // ✅ Nếu comment depth=0 → có thể có reply 2 tầng, nên xóa cascade thêm 1 lớp
    if (comment.depth === 0) {
      const level1Replies = await Comment.find({
        parentID: id
      }).select("_id");
      const level1IDs = level1Replies.map((r) => r._id);
      await Comment.deleteMany({
        parentID: {
          $in: level1IDs
        }
      });
    }

    // ✅ Giảm số comment trong Post
    await Post.findByIdAndUpdate(comment.postID, {
      $inc: {
        comment: -1
      }
    });

    res.json({
      success: true,
      message: "Đã xóa bình luận 🗑️",
    });
  } catch (err) {
    console.error("❌ Lỗi khi xóa bình luận:", err);
    res.status(500).json({
      success: false,
      message: err.message
    });
  }
});

// 🧩 POST /api/post/comment/report
router.post("/report", async (req, res) => {
  try {
    const author = req.header("x-user-id");
    const { target, content } = req.body;

    if (!author || !target) {
      return res.status(400).json({
        success: false,
        message: "Thiếu author (header x-user-id) hoặc target (commentID)"
      });
    }

    // 1️⃣ Kiểm tra comment có tồn tại không
    const existComment = await Comment.findById(target);

    if (!existComment) {
      return res.status(404).json({
        success: false,
        message: "Comment không tồn tại hoặc đã bị xóa!"
      });
    }
    const reportedUser = existComment.userID;

    // 2️⃣ Payload chuẩn gửi sang Static Service
    const payload = {
      author,
      reportedUser,
      type: "comment",
      target,
      content: content || ""
    };

    // 3️⃣ Gửi message vào RabbitMQ
    const channel = getChannel();
    if (!channel) {
      console.error("❌ Không thể gửi RabbitMQ: Channel chưa có!");
      return res.status(500).json({
        success: false,
        message: "Không thể gửi RabbitMQ"
      });
    }

    const QUEUE = process.env.RABBITMQ_STATS_QUEUE || "stats_queue";

    console.log("📤 Sending COMMENT REPORT to RabbitMQ:", payload);

    channel.sendToQueue(
      QUEUE,
      Buffer.from(JSON.stringify(payload)),
      { persistent: true }
    );

    return res.json({
      success: true,
      message: "Report comment đã được gửi vào hàng đợi",
    });

  } catch (err) {
    console.error("❌ Lỗi khi gửi report comment:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;