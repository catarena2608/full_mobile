const Follow = require("../models/followModel");
const { getChannel } = require("../config/rabbitmq");

async function startFollowersRPC() {
  const reqQueue = process.env.RABBITMQ_USER_QUEUE || "user_followers_queue";
  const resQueue = process.env.RABBITMQ_NOTIFY_RESPONSE_QUEUE || "user_response_queue";

  let channel = getChannel(reqQueue);
  while (!channel) {
    console.log("⏳ Waiting Rabbit channel...");
    await new Promise(r => setTimeout(r, 1000));
    channel = getChannel(reqQueue);
  }

  await channel.assertQueue(reqQueue, { durable: true });
  console.log("👂 User Service RPC listening at:", reqQueue);

  channel.consume(reqQueue, async (msg) => {
    if (!msg) return;

    const correlationId = msg.properties.correlationId;
    const replyTo = msg.properties.replyTo;

    const data = JSON.parse(msg.content.toString());
    console.log("📥 UserService RPC request:", data);

    if (data.type === "get_followers") {
      const followers = await Follow.find({ to: data.actorId });

      const userIDs = followers.map(f => f.from);

      await channel.sendToQueue(
        replyTo || resQueue,
        Buffer.from(JSON.stringify({
          actorId: data.actorId,
          followers: userIDs
        })),
        { correlationId }
      );
    }

    channel.ack(msg);
  });
}

module.exports = startFollowersRPC;
