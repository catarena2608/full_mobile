const TopSearch = require("../models/topSearchModel");
const Search = require("../models/searchModel");
const Report = require("../models/reportModel");
const Violate = require("../models/violationModel");
const { getChannel } = require("../config/rabbitmq");

async function startConsumer() {
  const QUEUE = process.env.RABBITMQ_STATS_QUEUE || "stats_queue";
  const channel = getChannel();

  if (!channel) {
    console.error("❌ Stats Consumer Error: Channel chưa sẵn sàng!");
    return;
  }

  console.log("📥 Stats Consumer đang chờ message...");

  channel.consume(
    QUEUE,
    async (msg) => {
      if (!msg) return;

      try {
        const payload = JSON.parse(msg.content.toString());
        console.log("📩 Nhận message:", payload);

        /* -----------------------------
         *  CASE 1: SEARCH EVENT
         * ----------------------------- */
        if (payload.keyword && Array.isArray(payload.type)) {
          await handleSearchEvent(payload);
          channel.ack(msg);
          return;
        }

        /* -----------------------------
         *  CASE 2: REPORT EVENT
         * ----------------------------- */
        if (
          payload.author &&
          ["comment", "post", "user"].includes(payload.type)
        ) {
          await handleReport(payload);
          channel.ack(msg);
          return;
        }
        /* -------------------------
         * CASE 3: CHECK VIOLATION
         * payload = { userID, type }
         * ------------------------- */
        if (payload.check?.startsWith("violation_")) {

  const type = payload.check.replace("violation_", ""); // post / comment / user

  const result = await handleCheckViolation({
    userID: payload.userID,
    type
  });

  if (msg.properties.replyTo) {
    channel.sendToQueue(
      msg.properties.replyTo,
      Buffer.from(JSON.stringify(result)),
      { correlationId: msg.properties.correlationId }
    );
  }

  channel.ack(msg);
  return;
}
        console.warn("⚠️ Không nhận dạng được payload:", payload);
        channel.ack(msg);

      } catch (err) {
        console.error("❌ Lỗi xử lý message:", err);
        channel.nack(msg, false, true);
      }
    },
    { noAck: false }
  );
}


/* =======================================================
   🧠 Handler 1: Search Event
======================================================== */
async function handleSearchEvent({ keyword, type, target }) {
  console.log("🔍 Xử lý SEARCH event...");

  const newSearch = await Search.create({
    keyword,
    type,
    target: Array.isArray(target) ? target : []
  });

  console.log("💾 Đã lưu Search");

  if (Array.isArray(newSearch.target)) {
    for (const t of newSearch.target) {
      const exist = await TopSearch.findOne({ target: t });

      if (exist) {
        const newTypes = Array.from(new Set([...exist.type, ...type]));
        await TopSearch.updateOne(
          { target: t },
          { $inc: { count: 1 }, type: newTypes }
        );
      } else {
        await TopSearch.create({
          target: t,
          type,
          count: 1
        });
      }
    }
  }

  console.log("📈 Đã cập nhật TopSearch!");
}


/* =======================================================
   🧠 Handler 2: Report Event
======================================================== */
async function handleReport({ author, target, content, type, reportedUser }) {
  console.log("🚨 Xử lý REPORT event...");

  await Report.create({
    author,
    reportedUser,
    target,
    type,
    content: content || ""
  });

  console.log("📝 Đã lưu report!");
}

/* =======================================================
   🧠 Handler 3: Violate Event
======================================================== */
async function handleCheckViolation({ userID, type }) {
  console.log("🚨 Kiểm tra VIOLATION:", userID, type);

  // Tìm violation BAN mới nhất
  const violation = await Violate.findOne({
    userID,
    type,
    action: "ban"
  })
    .sort({ createdAt: -1 })
    .lean();

  if (!violation) {
    return { expired: true, expireAt: null };  // Không có ban => OK
  }

  if (!violation.expiredAt) {
    return { expired: true, expireAt: null };
  }

  const now = Date.now();
  const expired = violation.expiredAt.getTime() <= now;

  return {
    expired,                // true = hết ban, false = đang ban
    expireAt: violation.expiredAt,
    action: violation.action
  };
}

module.exports = startConsumer;
