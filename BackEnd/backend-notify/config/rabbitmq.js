require("dotenv").config();
const amqp = require("amqplib");

let connection = null;
const channels = {};

async function connectRabbitMQ() {
  try {
    const RABBIT_URL = process.env.RABBITMQ_URL || "amqp://localhost";
    const PREFETCH = Number(process.env.RABBITMQ_PREFETCH || 10);

    connection = await amqp.connect(RABBIT_URL);
    console.log("🐰 RabbitMQ connected:", RABBIT_URL);

    // Tạo channel cho notification_queue
    const notifyQueue = process.env.RABBITMQ_NOTIFY_QUEUE || "notification_queue";
    console.log(`📌 Creating channel for notify queue: "${notifyQueue}"`); // 🔥 LOG
    
    const notifyChannel = await connection.createChannel();
    await notifyChannel.assertQueue(notifyQueue, { durable: true });
    notifyChannel.prefetch(PREFETCH);
    channels[notifyQueue] = notifyChannel;
    console.log(`✅ Channel created and stored with key: "${notifyQueue}"`); // 🔥 LOG

    // Tạo channel cho queue nhận followers từ User Service
    const userQueue = process.env.RABBITMQ_USER_QUEUE || "user_followers_queue";
    console.log(`📌 Creating channel for user queue: "${userQueue}"`); // 🔥 LOG
    
    const userChannel = await connection.createChannel();
    await userChannel.assertQueue(userQueue, { durable: true });
    userChannel.prefetch(PREFETCH);
    channels[userQueue] = userChannel;
    console.log(`✅ Channel created and stored with key: "${userQueue}"`); // 🔥 LOG

    console.log("📋 Available channel keys:", Object.keys(channels)); // 🔥 LOG

    // reconnect nếu connection bị đóng
    connection.on("close", () => {
      console.error("🔥 RabbitMQ connection closed. Reconnecting...");
      Object.keys(channels).forEach(k => delete channels[k]);
      connection = null;
      setTimeout(connectRabbitMQ, 3000);
    });

    connection.on("error", (err) => {
      console.error("🐞 RabbitMQ error:", err);
    });

    return channels;
  } catch (err) {
    console.error("❌ RabbitMQ Connection Error:", err.message);
    setTimeout(connectRabbitMQ, 5000);
  }
}

// Lấy channel theo queue name
function getChannel(queueName) {
  console.log(`🔍 getChannel called with: "${queueName}"`); // 🔥 LOG
  console.log(`📋 Available channels:`, Object.keys(channels)); // 🔥 LOG
  
  const channel = channels[queueName];
  
  if (!channel) {
    console.warn(`⚠️ Channel not found for: "${queueName}"`);
  } else {
    console.log(`✅ Channel found for: "${queueName}"`);
  }
  
  return channel;
}

module.exports = { connectRabbitMQ, getChannel };