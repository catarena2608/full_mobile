const Notify = require("../models/notifyModel");
const {
  getFollowersRPC
} = require("../utils/rpcToUser");
const {
  getChannel
} = require("../config/rabbitmq");
const {
  emitToUser
} = require("../sockets/socket");

async function startNotifyConsumer() {
  let channel = getChannel(process.env.RABBITMQ_NOTIFY_QUEUE);
  while (!channel) { // chờ channel sẵn sàng
    console.log("⏳ Waiting for RabbitMQ channel...");
    await new Promise(res => setTimeout(res, 1000));
    channel = getChannel(process.env.RABBITMQ_NOTIFY_QUEUE);
  }

  const queueName = process.env.RABBITMQ_NOTIFY_QUEUE || "notification_queue";
  await channel.assertQueue(queueName, {
    durable: true
  });

  console.log("🐰 Notification Consumer waiting for messages...");

  channel.consume(queueName, async (msg) => {
    if (!msg) return;

    try {
      const data = JSON.parse(msg.content.toString());
      console.log("Received event:", data);

      const {
        actorId,
        type,
        targetId,
        userID
      } = data;

      // ⚡ NEW_POST handling
      if (type === "new_post") {
        console.log("📨 RPC → UserService: get followers");
        const {
          followers
        } = await getFollowersRPC(actorId);
        console.log("👥 Followers:", followers);

        for (const uid of followers) {
          const notifyDoc = await Notify.create({
            userID: uid,
            actorID: actorId,
            type: "new_post",
            targetID: targetId
          });

          emitToUser(uid, {
            _id: notifyDoc._id,
            type: "new_post",
            actorID: actorId,
            targetID: targetId,
            createdAt: notifyDoc.createdAt
          });
        }

        channel.ack(msg);
        return;
      }

      // ⚡ REMOVE_POST
      if (type === "remove_post") {
        const notifyDoc = await Notify.create({
          userID, // chính chủ post
          actorID: actorId,
          type: "remove_post",
          targetID: targetId
        });

        emitToUser(userID, {
          _id: notifyDoc._id,
          type: "remove_post",
          actorID: actorId,
          targetID: targetId,
          createdAt: notifyDoc.createdAt
        });

        channel.ack(msg);
        return;
      }

      // ⚡ LIKE / UNLIKE
      if (type === "like") {
        const notifyDoc = await Notify.create({
          userID: userID,
          actorID: actorId,
          type,
          targetID: targetId
        });

        emitToUser(userID, {
          _id: notifyDoc._id,
          type,
          actorID: actorId,
          targetID: targetId,
          createdAt: notifyDoc.createdAt
        });
        channel.ack(msg);
        return;
      }

      // ⚡ COMMENT / REPLY
      if (type === "comment" || type === "reply") {
        // Nếu comment → notify chủ post
        const notifyDoc1 = await Notify.create({
          userID: userID, // userID gửi lên phải là chủ post
          actorID: actorId,
          type: "comment",
          targetID: targetId
        });

        emitToUser(userID, {
          _id: notifyDoc1._id,
          type: "comment",
          actorID: actorId,
          targetID: targetId,
          createdAt: notifyDoc1.createdAt
        });

        // Nếu là reply → thêm notify chủ comment gốc
        if (type === "reply" && data.replyToUserID) {
          const notifyDoc2 = await Notify.create({
            userID: data.replyToUserID,
            actorID: actorId,
            type: "reply",
            targetID
          });

          emitToUser(data.replyToUserID, {
            _id: notifyDoc2._id,
            type: "reply",
            actorID: actorId,
            targetID,
            createdAt: notifyDoc2.createdAt
          });
        }

        channel.ack(msg);
        return;
      }
      // ⚡ WARN / BAN
      if (type.startsWith("warn_") || type.startsWith("ban_")) {
        const notifyDoc = await Notify.create({
          userID,
          actorID: actorId,
          type, // ví dụ: warn_user
          targetID: targetId
        });

        emitToUser(userID, {
          _id: notifyDoc._id,
          type,
          actorID: actorId,
          targetID: targetId,
          createdAt: notifyDoc.createdAt
        });

        channel.ack(msg);
        return;
      }
      // ⚡ FOLLOW
      if (type === "follow") {
        const notifyDoc = await Notify.create({
          userID, // người được follow
          actorID: actorId,
          type: "follow",
          targetID: targetId
        });

        emitToUser(userID, {
          _id: notifyDoc._id,
          type: "follow",
          actorID: actorId,
          targetID: targetId,
          createdAt: notifyDoc.createdAt
        });

        channel.ack(msg);
        return;
      }

      // 🔹 Nếu type khác
      channel.ack(msg);

    } catch (err) {
      console.error("❌ Error processing notification:", err);
      channel.ack(msg);
    }
  });
}

module.exports = startNotifyConsumer;