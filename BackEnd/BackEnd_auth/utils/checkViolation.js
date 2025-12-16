const { getChannel, pendingResponses } = require("../config/rabbitmq");
const { v4: uuidv4 } = require("uuid");

function sendRPC(queue, message) {
  return new Promise((resolve, reject) => {
    console.log("[RPC] 🚀 Bắt đầu sendRPC");
    console.log("[RPC] 📤 Queue:", queue);
    console.log("[RPC] 📦 Message:", message);

    const channel = getChannel();
    if (!channel) {
      console.error("[RPC] ❌ Channel chưa sẵn sàng");
      return reject("Channel chưa sẵn sàng");
    }

    const correlationId = uuidv4();
    console.log("[RPC] 🔑 correlationId:", correlationId);

    // Lưu resolver để khi có reply thì resolve
    pendingResponses[correlationId] = (response) => {
      console.log("[RPC] ✅ Nhận response cho correlationId:", correlationId);
      console.log("[RPC] 📥 Response:", response);
      resolve(response);
    };

    try {
      channel.sendToQueue(
        queue,
        Buffer.from(JSON.stringify(message)),
        {
          replyTo: "amq.rabbitmq.reply-to",
          correlationId,
        }
      );

      console.log("[RPC] 📬 Đã gửi message lên RabbitMQ");
    } catch (err) {
      console.error("[RPC] ❌ Lỗi khi sendToQueue:", err);
      delete pendingResponses[correlationId];
      reject(err);
    }
  });
}

async function checkViolation(userID, type) {
  console.log("[checkViolation] 🔍 Bắt đầu kiểm tra vi phạm");
  console.log("[checkViolation] 👤 userID:", userID);
  console.log("[checkViolation] 🏷️ type:", type);

  const queue = process.env.RABBITMQ_STATS_QUEUE || "stats_queue";
  console.log("[checkViolation] 📤 Queue sử dụng:", queue);

  const payload = {
    userID,
    check: `violation_${type}`,
  };

  console.log("[checkViolation] 📦 Payload:", payload);

  const result = await sendRPC(queue, payload);

  console.log("[checkViolation] 🎯 Kết quả cuối:", result);
  return result;
}

module.exports = { checkViolation };
