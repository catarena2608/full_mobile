const TopSearch = require("../models/topSearchModel");
const Search = require("../models/searchModel");

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
        const content = JSON.parse(msg.content.toString());
        console.log("📩 Nhận event search từ queue:", content);

        const { keyword, type, target } = content;

        if (!keyword || !Array.isArray(type)) {
          console.error("⚠️ Payload không hợp lệ:", content);
          channel.ack(msg);
          return;
        }

        /* -------------------------------
         * 1️⃣ Lưu vào Search collection
         * ------------------------------- */
        const newSearch = await Search.create({
          keyword,
          type,
          target: Array.isArray(target) ? target : []
        });

        console.log("💾 Đã lưu search event!");

        /* -------------------------------
         * 2️⃣ Cập nhật TopSearch
         * ------------------------------- */
        if (Array.isArray(newSearch.target)) {
          for (const t of newSearch.target) {
            const exist = await TopSearch.findOne({ target: t });

            if (exist) {
              // merge type, không trùng
              const newTypes = Array.from(new Set([...exist.type, ...type]));

              await TopSearch.findOneAndUpdate(
                { target: t },
                {
                  $inc: { count: 1 },
                  type: newTypes
                },
                { new: true }
              );
            } else {
              // tạo mới
              await TopSearch.create({
                target: t,
                type,
                count: 1
              });
            }
          }
        }

        console.log("📈 Đã cập nhật TopSearch!");

        channel.ack(msg);
      } catch (err) {
        console.error("❌ Lỗi xử lý message:", err.message);
        channel.nack(msg, false, true);
      }
    },
    { noAck: false }
  );
}

module.exports = startConsumer;
