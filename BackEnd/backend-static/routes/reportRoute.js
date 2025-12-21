const express = require("express");
const router = express.Router();
const Report = require("../models/reportModel");
const Violate = require("../models/violationModel");
const { getChannel } = require("../config/rabbitmq");
// 📌 GET: Lấy report theo filter
router.get("/", async (req, res) => {
  try {
    const { type, target, day, month, year } = req.query;

    let filter = {};

    // ------------------------------
    // 1️⃣ Filter theo loại (user/post/comment)
    // ------------------------------
    if (type) filter.type = type;

    // ------------------------------
    // 2️⃣ Filter theo ID đối tượng bị report
    // ------------------------------
    if (target) filter.target = target;

    // ------------------------------
    // 3️⃣ Filter thời gian (tùy chọn)
    // ------------------------------
    if (day || month || year) {
      let start = new Date();
      let end = new Date();

      // Nếu chỉ có year
      if (year && !month && !day) {
        start = new Date(year, 0, 1);
        end = new Date(Number(year) + 1, 0, 1);
      }

      // Nếu có month + year
      if (year && month && !day) {
        start = new Date(year, Number(month) - 1, 1);
        end = new Date(year, Number(month), 1);
      }

      // Nếu có đầy đủ day + month + year
      if (year && month && day) {
        start = new Date(year, Number(month) - 1, Number(day));
        end = new Date(year, Number(month) - 1, Number(day) + 1);
      }

      // Nếu chỉ có month → mặc định năm hiện tại
      if (!year && month && !day) {
        const y = new Date().getFullYear();
        start = new Date(y, Number(month) - 1, 1);
        end = new Date(y, Number(month), 1);
      }

      // Nếu chỉ có day → mặc định tháng + năm hiện tại
      if (day && !month && !year) {
        const now = new Date();
        start = new Date(now.getFullYear(), now.getMonth(), Number(day));
        end = new Date(now.getFullYear(), now.getMonth(), Number(day) + 1);
      }

      filter.createdAt = { $gte: start, $lt: end };
    }

    // ------------------------------
    // 4️⃣ Thực thi truy vấn
    // ------------------------------
    const reports = await Report.find(filter).sort({ createdAt: -1 });

    return res.json({
      success: true,
      filters: { type, target, day, month, year },
      total: reports.length,
      reports
    });

  } catch (err) {
    console.error("❌ Lỗi lấy report:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

// 🧩 POST /api/admin/violate
router.post("/violate", async (req, res) => {
  try {
    const {
      userID,
      action,
      type,
      target,
      reason="",
      end=true,
      expiredAt
    } = req.body;

    if (!userID || !action) {
      return res.status(400).json({
        success: false,
        message: "Thiếu userID hoặc action"
      });
    }

    // 1️⃣ Lưu DB violation
    const violation = await Violate.create({
      userID,
      action,
      type: type || "",
      target: target || "",
      reason: reason || "",
      end: typeof end === "boolean" ? end : true,
      expiredAt: expiredAt ? new Date(expiredAt) : undefined
    });

    // 2️⃣ Lấy channel
    const channel = getChannel();
    if (!channel) {
      return res.status(500).json({
        success: false,
        message: "Channel RabbitMQ không tồn tại"
      });
    }

    const QUEUE_POST = process.env.RABBITMQ_POSTVIO_QUEUE || "violate_post_queue";
    const QUEUE_USER = process.env.RABBITMQ_USERVIO_QUEUE || "violate_user_queue";
    const QUEUE_NOTIFY = process.env.RABBITMQ_NOTIFY_QUEUE || "notification_queue";

    // 3️⃣ Chọn queue post/user service
    let queueToSend;
    if (type === "post" || type === "comment") {
      queueToSend = QUEUE_POST;
    } else if (type === "user") {
      queueToSend = QUEUE_USER;
    } else {
      queueToSend = "violate_other_queue";
    }

    const payload = {
      event: "violation",
      userID,
      action,
      type,
      target,
      reason,
      end,
      expiredAt
    };

    // Gửi violation đến queue
    channel.sendToQueue(queueToSend, Buffer.from(JSON.stringify(payload)), { persistent: true });

    // 4️⃣ 🔥 Gửi notify cho user bị phạt
    const notifyType = `${action}_${type || "user"}`; 
    // warn_user, warn_post, warn_comment OR ban_user, ban_post, ban_comment

    const notifyPayload = {
      actorId: "System",
      userID: userID,      // người bị phạt → người nhận notify
      type: notifyType,    // warn_user / ban_user / warn_post ...
      targetId: target
    };

    channel.sendToQueue(QUEUE_NOTIFY, Buffer.from(JSON.stringify(notifyPayload)), {
      persistent: true
    });

    return res.json({
      success: true,
      message: `Đã gửi VIOLATE vào ${queueToSend} và notify user`,
      data: violation
    });

  } catch (err) {
    console.error("❌ Lỗi tạo violate:", err);
    return res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;