require("dotenv").config();
const express = require("express");
const mongoose = require("mongoose");
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcryptjs");

const { connectRabbitMQ } = require("./config/rabbitmq");
const startConsumer = require("./services/consumer");

const Admin = require("./models/adminModel");
const searchRoute = require("./routes/searchRoute");
const loginRoute = require("./routes/loginRoute");
const reportRoute = require("./routes/reportRoute");

const app = express();
app.use(express.json());

const PORT = process.env.PORT || 7001;
const MONGO_URI = process.env.MONGO_URI;

// --------------------------------------------------
// 🟦 IMPORT ADMIN.JSON NẾU DB RỖNG
// --------------------------------------------------
async function importAdminsIfEmpty() {
  const count = await Admin.countDocuments();

  if (count > 0) {
    console.log("🔹 Admin DB đã có dữ liệu → bỏ qua import.");
    return;
  }

  console.log("📥 Import admin.json vào DB...");

  const filePath = path.join(__dirname, "backups", "admin.json");

  if (!fs.existsSync(filePath)) {
    console.error("⚠️ Không tìm thấy backups/admin.json!");
    return;
  }

  const raw = fs.readFileSync(filePath, "utf-8");
  const admins = JSON.parse(raw);

  for (const item of admins) {
    const hashed = await bcrypt.hash(item.password, 10);
    item.password = hashed;
    await Admin.create(item);
  }

  console.log("✅ Import admin.json hoàn tất!");
}

// --------------------------------------------------
// START SERVER
// --------------------------------------------------
async function startServer() {
  try {
    await mongoose.connect(MONGO_URI, { autoIndex: true });
    console.log("🍃 MongoDB connected (Stats DB)");

    // 👉 Import admin nếu rỗng
    await importAdminsIfEmpty();

    // RabbitMQ
    await connectRabbitMQ();
    setTimeout(() => startConsumer(), 500);

    // Routes
    app.use("/stat/login", loginRoute);
    app.use("/stat/search", searchRoute);
    app.use("/stat/report",reportRoute);

    // Start
    app.listen(PORT, () => {
      console.log(`🚀 Stats Service chạy tại port ${PORT}`);
    });

  } catch (err) {
    console.error("❌ Stats Service Startup Error:", err.message);
    process.exit(1);
  }
}

startServer();
