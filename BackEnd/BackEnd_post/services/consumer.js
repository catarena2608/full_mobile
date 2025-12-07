require("dotenv").config();
const { getChannel, connectRabbitMQ } = require("../config/rabbitmq");

const { deletePostCascade } = require("./postDelete");
const { deleteCommentCascade } = require("./commentDelete");

async function startPostConsumer() {
  let channel = getChannel() || await connectRabbitMQ();

  if (!channel) {
    console.error("❌ Không thể tạo channel để consume");
    return;
  }

  const QUEUE = process.env.RABBITMQ_POSTVIO_QUEUE || "violate_post_queue";
  await channel.assertQueue(QUEUE, { durable: true });

  console.log(`👂 Listening: ${QUEUE}`);

  channel.consume(
    QUEUE,
    async (msg) => {
      if (!msg) return;

      try {
        const data = JSON.parse(msg.content.toString());
        const { type, target } = data;

        if (!type || !target) {
          console.log("⚠️ Message thiếu type hoặc target");
          return channel.ack(msg);
        }

        // =========================
        // 🔥 DELETE POST
        // =========================
        if (type === "post") {
          const result = await deletePostCascade(target);
          console.log("🗑️ Post Cascade Result:", result);
          return channel.ack(msg);
        }

        // =========================
        // 🔥 DELETE COMMENT
        // =========================
        if (type === "comment") {
          const result = await deleteCommentCascade(target);
          console.log("🗑️ Comment Cascade Result:", result);
          return channel.ack(msg);
        }

        console.warn("⚠️ Loại không hợp lệ:", type);
        channel.ack(msg);

      } catch (err) {
        console.error("❌ ERROR:", err);
      }
    },
    { noAck: false }
  );
}

module.exports = startPostConsumer;
