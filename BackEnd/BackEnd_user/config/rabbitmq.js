require("dotenv").config();
const amqp = require("amqplib");

let connection = null;
const channels = {};

async function createQueueChannel(queue, prefetch) {
  const ch = await connection.createChannel();
  await ch.assertQueue(queue, { durable: true });
  ch.prefetch(prefetch);
  channels[queue] = ch;
}

async function connectRabbitMQ() {
  try {
    const RABBIT_URL = process.env.RABBITMQ_URL || "amqp://localhost";
    const PREFETCH = Number(process.env.RABBITMQ_PREFETCH || 10);

    connection = await amqp.connect(RABBIT_URL);
    console.log("🐰 RabbitMQ connected (User Service):", RABBIT_URL);

    const queues = [
      process.env.RABBITMQ_NOTIFY_QUEUE || "notification_queue",
      process.env.RABBITMQ_USER_QUEUE || "user_followers_queue",
      process.env.RABBITMQ_USERVIO_QUEUE || "violate_user_queue",
      process.env.RABBITMQ_STATS_QUEUE || "stats_queue",
    ];

    for (const q of queues) {
      await createQueueChannel(q, PREFETCH);
    }

    connection.on("close", () => {
      console.error("🔥 User RabbitMQ connection closed. Reconnecting...");
      Object.keys(channels).forEach(k => delete channels[k]);
      connection = null;
      setTimeout(connectRabbitMQ, 3000);
    });

    connection.on("error", (err) => {
      console.error("🐞 User RabbitMQ error:", err);
    });

    return channels;

  } catch (err) {
    console.error("❌ User RabbitMQ Connection Error:", err.message);
    setTimeout(connectRabbitMQ, 5000);
  }
}

function getChannel(queueName) {
  return channels[queueName];
}

module.exports = { connectRabbitMQ, getChannel };
