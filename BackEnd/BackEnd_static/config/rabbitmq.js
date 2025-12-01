require("dotenv").config();
const amqp = require("amqplib");

let connection = null;
let channel = null;

async function connectRabbitMQ() {
  try {
    const RABBIT_URL = process.env.RABBITMQ_URL || "amqp://localhost";
    const QUEUE = process.env.RABBITMQ_STATS_QUEUE || "stats_queue";
    const PREFETCH = Number(process.env.RABBITMQ_PREFETCH || 20);

    connection = await amqp.connect(RABBIT_URL);
    channel = await connection.createChannel();

    console.log("📊🐰 Stats RabbitMQ connected:", RABBIT_URL);

    await channel.assertQueue(QUEUE, { durable: true });
    channel.prefetch(PREFETCH);

    connection.on("close", () => {
      console.error("🔥 Stats RabbitMQ connection closed. Reconnecting...");
      channel = null;
      connection = null;
      setTimeout(connectRabbitMQ, 3000);
    });

    connection.on("error", (err) => {
      console.error("🐞 Stats RabbitMQ error:", err);
    });

    return channel;
  } catch (err) {
    console.error("❌ Stats RabbitMQ Connection Error:", err.message);
    setTimeout(connectRabbitMQ, 5000);
  }
}

function getChannel() {
  return channel;
}

module.exports = { connectRabbitMQ, getChannel };
