const { sendRPC } = require("../config/rabbitmq");

async function checkViolation(userID, check) {
  return await sendRPC(
    process.env.RABBITMQ_STATS_QUEUE || "stats_queue",
    { userID, check }
  );
}

module.exports = { checkViolation };
