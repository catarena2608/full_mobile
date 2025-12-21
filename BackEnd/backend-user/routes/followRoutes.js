const express = require("express");
const router = express.Router();
const Follow = require("../models/followModel");
const CountFollow = require("../models/countFollowModel");
const User = require("../models/userModel");
const { getChannel } = require("../config/rabbitmq");

async function updateCountFollowByDate(date, delta) {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;

  await CountFollow.findOneAndUpdate(
    { year, month },
    { $inc: { count: delta } },
    { upsert: true } // chưa có thì tự tạo
  );
}

// Dùng middleware auth
const auth = require("../utils/checkHeader");

/* ----------------------------- FOLLOW ----------------------------- */
// POST /follow/:targetId
router.post("/:targetId", auth, async (req, res) => {
  try {
    const from = req.user.userID;
    const to = req.params.targetId;

    if (from === to)
      return res.status(400).json({ message: "Không thể follow chính mình" });

    const followId = `${from}-${to}`;

    // Tạo theo timestamps → Mongo sẽ tự tạo createdAt
    const followDoc = await Follow.create({ _id: followId, from, to });

    // Cập nhật count theo createdAt
    await updateCountFollowByDate(followDoc.createdAt, +1);

    // Cập nhật số phía user
    await User.findByIdAndUpdate(from, { $inc: { numFollowing: 1 } });
    await User.findByIdAndUpdate(to, { $inc: { numFollowed: 1 } });

    // 🔔 Push event vào RabbitMQ với postOwner
    const QUEUE = process.env.RABBITMQ_NOTIFY_QUEUE || "notification_queue";
    const channel = getChannel(QUEUE);

    if (channel) {
      const payload = {
        actorId: from,
        type: "follow",
        targetId: to,
        userID: to
      };
      console.log("📤 Sending LIKE event to RabbitMQ:", payload);
      channel.sendToQueue(QUEUE, Buffer.from(JSON.stringify(payload)), { persistent: true });
    }

    res.json({ success: true, message: "Follow thành công" });

  } catch (err) {
    if (err.code === 11000) {
      return res.json({ success: false, message: "Đã follow trước đó" });
    }
    res.status(500).json({ message: err.message });
  }
});

/* ---------------------------- UNFOLLOW ---------------------------- */
// DELETE /follow/:targetId
router.delete("/:targetId", auth, async (req, res) => {
  try {
    const from = req.user.userID;
    const to = req.params.targetId;
    const followId = `${from}-${to}`;

    // Lấy doc trước khi xóa (để biết createdAt)
    const followDoc = await Follow.findById(followId);

    if (!followDoc) {
      return res.json({ success: true, message: "Bạn chưa follow người này" });
    }

    // Xóa relation
    await Follow.findByIdAndDelete(followId);

    // Giảm thống kê theo tháng-năm đã follow
    await updateCountFollowByDate(followDoc.createdAt, -1);

    // Giảm số lượng user
    await User.findByIdAndUpdate(from, { $inc: { numFollowing: -1 } });
    await User.findByIdAndUpdate(to, { $inc: { numFollowed: -1 } });

    res.json({ success: true, message: "Unfollow thành công" });

  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

module.exports = router;
