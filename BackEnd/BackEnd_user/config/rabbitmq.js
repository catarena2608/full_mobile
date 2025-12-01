require("dotenv").config();
const amqp = require("amqplib");

let connection = null;
const channels = {}; //lưu nhiều channel theo tên queue

async function connectRabbitMQ() {
  try {
    const RABBIT_URL = process.env.RABBITMQ_URL || "amqp://localhost";
    const PREFETCH = Number(process.env.RABBITMQ_PREFETCH || 10);

    connection = await amqp.connect(RABBIT_URL);
    console.log("🐰 RabbitMQ connected (User Service):", RABBIT_URL);

    // Tạo channel cho notification_queue
    const notifyQueue = process.env.RABBITMQ_NOTIFY_QUEUE || "notification_queue";
    const notifyChannel = await connection.createChannel();
    await notifyChannel.assertQueue(notifyQueue, { durable: true });
    notifyChannel.prefetch(PREFETCH);
    channels[notifyQueue] = notifyChannel;

    // Tạo channel cho queue nhận followers từ User Service
    const userQueue = process.env.RABBITMQ_USER_QUEUE || "user_followers_queue";
    const userChannel = await connection.createChannel();
    await userChannel.assertQueue(userQueue, { durable: true });
    userChannel.prefetch(PREFETCH);
    channels[userQueue] = userChannel;

    // Queue mới cho thống kê
    const statsQueue = process.env.RABBITMQ_STATS_QUEUE || "stats_queue";
    const statsChannel = await connection.createChannel();
    await statsChannel.assertQueue(statsQueue, { durable: true });
    statsChannel.prefetch(PREFETCH);
    channels[statsQueue] = statsChannel;

    // reconnect nếu connection bị đóng
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

// Lấy channel theo queue name
function getChannel(queueName) {
  return channels[queueName];
}

module.exports = { connectRabbitMQ, getChannel };
