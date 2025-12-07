require("dotenv").config();
const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");
const bodyParser = require("body-parser");
const fs = require("fs");

const { connectRabbitMQ } = require("./config/rabbitmq");
const startFollowersRPC = require("./services/followConsumer");
const startViolateConsumer = require("./services/violateConsumer");

const app = express();

app.use(cors({ origin: "http://localhost:5173", credentials: true }));
app.use(bodyParser.json());

// ===== ROUTES =====
app.use("/api/users", require("./routes/userRoutes"));
app.use("/api/follow", require("./routes/followRoutes"));
app.use("/api/userAdmin", require("./routes/adminRoutes"));

// ===== MONGODB =====
mongoose.connect(process.env.MONGO_URI)
  .then(() => console.log("✅ User service connected to MongoDB"))
  .catch(err => console.error("❌ MongoDB connection error:", err));

const db = mongoose.connection;

// AUTO IMPORT JSON
db.once("open", async () => {
  try {
    const User = require("./models/userModel");
    const Follow = require("./models/followModel");

    if (await User.countDocuments() === 0 && fs.existsSync("./backups/users.json")) {
      await User.insertMany(JSON.parse(fs.readFileSync("./backups/users.json", "utf-8")));
      console.log("✅ Imported users.json vào MongoDB");
    }

    if (await Follow.countDocuments() === 0 && fs.existsSync("./backups/follows.json")) {
      const data = JSON.parse(fs.readFileSync("./backups/follows.json", "utf-8"));
      await Follow.insertMany(data.map(f => ({
        _id: `${f.from}-${f.to}`,
        from: f.from,
        to: f.to
      })));
      console.log("✅ Imported follows.json");
    }

    await rebuildStatsIfEmpty();

  } catch (err) {
    console.error("❌ Lỗi import dữ liệu:", err);
  }
});

// ===== AUTO BUILD STATS =====
async function rebuildStatsIfEmpty() {
  await buildStats(
    "countUserModel",
    require("./models/userModel"),
    ["year", "month"]
  );

  await buildStats(
    "countFollowModel",
    require("./models/followModel"),
    ["year", "month"]
  );
}

async function buildStats(modelName, sourceModel) {
  const Model = require(`./models/${modelName}`);
  if (await Model.countDocuments() > 0) return;

  console.log(`⚠️ ${modelName} trống → bắt đầu đếm lại...`);

  const data = await sourceModel.aggregate([
    {
      $group: {
        _id: {
          year: { $year: "$createdAt" },
          month: { $month: "$createdAt" }
        },
        count: { $sum: 1 }
      }
    },
    { $sort: { "_id.year": 1, "_id.month": 1 } }
  ]);

  if (!data.length) {
    console.log(`ℹ️ ${modelName} không có dữ liệu nguồn → bỏ qua.`);
    return;
  }

  await Model.insertMany(data.map(g => ({
    year: g._id.year,
    month: g._id.month,
    count: g.count
  })));

  console.log(`✅ Đã tạo dữ liệu thống kê ${modelName}`);
}

// ===== START RABBITMQ =====
async function startServices() {
  await connectRabbitMQ();
  await startFollowersRPC();
  await startViolateConsumer();
}

startServices();

const PORT = process.env.PORT || 3002;
app.listen(PORT, () => console.log(`🚀 User service chạy ở http://localhost:${PORT}`));
