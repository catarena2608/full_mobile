const { getChannel } = require("../config/rabbitmq");
const { v4: uuidv4 } = require("uuid");

function wait(ms) { return new Promise(r => setTimeout(r, ms)); }

async function getFollowersRPC(actorId) {
  const reqQueue = process.env.RABBITMQ_USER_QUEUE || "user_followers_queue";
  const resQueue = process.env.RABBITMQ_USER_RESPONSE_QUEUE || "user_response_queue";

  let channel = getChannel(reqQueue);
  while (!channel) {
    console.log("⏳ Notify waiting Rabbit channel...");
    await wait(1000);
    channel = getChannel(reqQueue);
  }

  await channel.assertQueue(resQueue, { durable: true });

  const correlationId = uuidv4();

  return new Promise((resolve) => {
    channel.consume(
      resQueue,
      (msg) => {
        if (msg.properties.correlationId === correlationId) {
          const data = JSON.parse(msg.content.toString());
          resolve(data);
        }
        channel.ack(msg);
      },
      { noAck: false }
    );

    channel.sendToQueue(
      reqQueue,
      Buffer.from(JSON.stringify({
        type: "get_followers",
        actorId
      })),
      { correlationId, replyTo: resQueue }
    );
  });
}

module.exports = { getFollowersRPC };
