require("dotenv").config();
const amqp = require("amqplib");

let channel = null;
let connection = null;
let pendingResponses = {}; // map correlationId → resolver

async function connectRabbitMQ() {
  try {
    const RABBIT_URL = process.env.RABBITMQ_URL || "amqp://localhost";
    const STATS_QUEUE = process.env.RABBITMQ_STATS_QUEUE || "stats_queue";
    const PREFETCH = Number(process.env.RABBITMQ_PREFETCH || 10);

    connection = await amqp.connect(RABBIT_URL);
    channel = await connection.createChannel();

    console.log("🐰 RabbitMQ connected (Post Service):", RABBIT_URL);

    // Queue mới cho thống kê
    await channel.assertQueue(STATS_QUEUE, { durable: true });

    // 🔥 Tạo consumer reply-to CHỈ MỘT LẦN
  channel.consume(
    "amq.rabbitmq.reply-to",
    (msg) => {
      const correlationId = msg.properties.correlationId;
      if (pendingResponses[correlationId]) {
        const resolver = pendingResponses[correlationId];
        resolver(JSON.parse(msg.content.toString()));
        delete pendingResponses[correlationId];
      }
    },
    { noAck: true }
  );

    channel.prefetch(PREFETCH);

    connection.on("close", () => {
      console.error("🔥 Post RabbitMQ connection closed. Reconnecting...");
      channel = null;
      connection = null;
      setTimeout(connectRabbitMQ, 3000);
    });

    connection.on("error", (err) => {
      console.error("🐞 Post RabbitMQ error:", err);
    });

    return channel;
  } catch (err) {
    console.error("❌ Post RabbitMQ Connection Error:", err.message);
    setTimeout(connectRabbitMQ, 5000);
  }
}

function getChannel() {
  return channel;
}

module.exports = { connectRabbitMQ, getChannel , pendingResponses};
