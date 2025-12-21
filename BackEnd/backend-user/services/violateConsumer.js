const User = require("../models/userModel");
const { getChannel } = require("../config/rabbitmq");

async function waitForChannel(q) {
  let ch = getChannel(q);
  while (!ch) {
    console.log("⏳ Waiting RabbitMQ channel for violateConsumer...");
    await new Promise(r => setTimeout(r, 1000));
    ch = getChannel(q);
  }
  return ch;
}

async function startViolateConsumer() {
  const QUEUE = process.env.RABBITMQ_USERVIO_QUEUE || "violate_user_queue";
  const channel = await waitForChannel(QUEUE);

  console.log("👂 User Service is listening VIOLATE_QUEUE:", QUEUE);

  channel.consume(QUEUE, async (msg) => {
    if (!msg) return;

    try {
      const data = JSON.parse(msg.content.toString());
      console.log("⚠️ Received VIOLATE:", data);

      const { userID } = data;
      if (!userID) {
        console.warn("⚠️ Violate message missing userID:", data);
        return channel.ack(msg);
      }

      const user = await User.findById(userID);
      if (!user) {
        console.warn("❌ User not found for violation:", userID);
        return channel.ack(msg);
      }

      // Dịch array tags xuống 1 và thêm "Violated" lên đầu
      user.tags = ["Violated", ...(user.tags || [])];
      await user.save();

      console.log(`✅ User ${userID} updated with tags[0] = "Violated"`);

      channel.ack(msg);

    } catch (err) {
      console.error("❌ Error handling violate:", err);
      channel.nack(msg, false, true);
    }
  }, { noAck: false });
}

module.exports = startViolateConsumer;
