const { getChannel } = require("../config/rabbitmq");
const { v4: uuidv4 } = require("uuid");
const { pendingResponses } = require("../config/rabbitmq")
function sendRPC(queue, message) {
  return new Promise(async (resolve, reject) => {
    const channel = getChannel();
    if (!channel) return reject("Channel chưa sẵn sàng");

    const correlationId = uuidv4();

    // Lưu resolver
    pendingResponses[correlationId] = resolve;

    channel.sendToQueue(
      queue,
      Buffer.from(JSON.stringify(message)),
      {
        replyTo: "amq.rabbitmq.reply-to",
        correlationId
      }
    );
  });
}

async function checkViolation(userID, type) {
  const queue = process.env.RABBITMQ_STATS_QUEUE || "stats_queue";

  return await sendRPC(queue, { 
    userID, 
    check: `violation_${type}`  // ví dụ: violation_user
  });
}

module.exports = { checkViolation };
