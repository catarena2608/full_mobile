require("dotenv").config();
const amqp = require("amqplib");
const { v4: uuidv4 } = require("uuid");

let connection = null;
let channel = null;
let responseHandlers = {}; // Map để lưu handler theo correlationId

async function connectRabbitMQ() {
  try {
    const RABBIT_URL = process.env.RABBITMQ_URL || "amqp://localhost";
    const NOTIFY_QUEUE = process.env.RABBITMQ_NOTIFY_QUEUE || "notification_queue";
    const STATS_QUEUE = process.env.RABBITMQ_STATS_QUEUE || "stats_queue";

    connection = await amqp.connect(RABBIT_URL);
    channel = await connection.createChannel();

    console.log("🐰 RabbitMQ connected (POST Service)");

    await channel.assertQueue(NOTIFY_QUEUE, { durable: true });
    await channel.assertQueue(STATS_QUEUE, { durable: true });

    // RPC Prefetch
    channel.prefetch(10);

    /* ----------------------------------------
       👉 ĐĂNG KÝ CONSUME REPLY-TO CHỈ 1 LẦN
    ---------------------------------------- */
    channel.consume(
      "amq.rabbitmq.reply-to",
      (msg) => {
        const correlationId = msg.properties.correlationId;
        if (responseHandlers[correlationId]) {
          const handler = responseHandlers[correlationId];
          handler(JSON.parse(msg.content.toString()));
          delete responseHandlers[correlationId];
        }
      },
      { noAck: true }
    );

    connection.on("close", () => {
      console.error("🔥 RabbitMQ closed! Reconnecting...");
      setTimeout(connectRabbitMQ, 3000);
    });

    connection.on("error", (err) => {
      console.error("🐞 RabbitMQ error:", err.message);
    });

    return channel;
  } catch (err) {
    console.error("❌ RabbitMQ Connect Error:", err.message);
    setTimeout(connectRabbitMQ, 3000);
  }
}

function getChannel() {
  return channel;
}

/* ==========================================================================
   RPC FUNCTION — không consume lại reply-to!
=========================================================================== */

function sendRPC(queue, message) {
  return new Promise((resolve, reject) => {
    const ch = getChannel();
    if (!ch) return reject("Channel chưa sẵn sàng");

    const correlationId = uuidv4();

    // Đăng ký callback cho correlationId
    responseHandlers[correlationId] = resolve;

    ch.sendToQueue(
      queue,
      Buffer.from(JSON.stringify(message)),
      {
        replyTo: "amq.rabbitmq.reply-to",
        correlationId,
      }
    );
  });
}

module.exports = { connectRabbitMQ, getChannel, sendRPC };
