const express = require("express");
const router = express.Router();
const Post = require("../models/postModel");
const Like = require("../models/likeModel");
const CountLike = require("../models/countLikeModel");
const { getChannel } = require("../config/rabbitmq");

const QUEUE = process.env.RABBITMQ_NOTIFY_QUEUE || "notification_queue";

// =========================
// ❤️ LIKE
// =========================
router.post("/", async (req, res) => {
  try {
    const userID = req.header("x-user-id");
    const { postID } = req.body;

    if (!userID || !postID)
      return res.status(400).json({ success: false, message: "Thiếu userID hoặc postID" });

    const likeID = `${userID}_${postID}`;

    const existed = await Like.findById(likeID);
    if (existed) return res.json({ success: true, message: "Đã like trước đó" });

    // ⬇️ Tạo Like (createdAt sinh ra tại đây)
    const newLike = await Like.create({ _id: likeID, userID, postID });

    // ---------------------------
    // 📊 UPDATE countLike
    // ---------------------------
    const createdAt = newLike.createdAt;
    const month = createdAt.getMonth() + 1;
    const year = createdAt.getFullYear();

    await CountLike.findOneAndUpdate(
      { month, year },
      { $inc: { count: 1 } },
      { upsert: true, new: true }
    );

    // Update post total likes
    const post = await Post.findByIdAndUpdate(postID, { $inc: { like: 1 } }, { new: true });
    if (!post) return res.status(404).json({ success: false, message: "Post không tồn tại" });

    // ---------------------------
    // 🔔 SEND RabbitMQ
    // ---------------------------
    const channel = getChannel();
    if (channel) {
      const payload = {
        actorId: userID,
        type: "like",
        targetId: postID,
        userID: post.userID,
      };
      channel.sendToQueue(QUEUE, Buffer.from(JSON.stringify(payload)), { persistent: true });
    }

    res.json({ success: true, message: "Đã like bài viết ❤️" });
  } catch (err) {
    console.error("❌ Lỗi khi like:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

// =========================
// 💔 UNLIKE
// =========================
router.delete("/", async (req, res) => {
  try {
    const userID = req.header("x-user-id");
    const { postID } = req.body;

    if (!userID || !postID)
      return res.status(400).json({ success: false, message: "Thiếu userID hoặc postID" });

    const likeID = `${userID}_${postID}`;

    // Xóa Like (lấy createdAt để biết tháng)
    const deleted = await Like.findByIdAndDelete(likeID);
    if (!deleted)
      return res.json({ success: true, message: "Chưa từng like bài này" });

    const createdAt = deleted.createdAt;
    const month = createdAt.getMonth() + 1;
    const year = createdAt.getFullYear();

    // ---------------------------
    // 📊 UPDATE countLike (giảm 1)
    // ---------------------------
    await CountLike.findOneAndUpdate(
      { month, year },
      { $inc: { count: -1 } },
      { new: true }
    );

    // Update post like count
    const post = await Post.findByIdAndUpdate(postID, { $inc: { like: -1 } }, { new: true });
    if (!post)
      return res.status(404).json({ success: false, message: "Post không tồn tại" });

    res.json({ success: true, message: "Đã bỏ like 💔" });
  } catch (err) {
    console.error("❌ Lỗi khi unlike:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
