require("dotenv").config();
const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");
const bodyParser = require("body-parser");
const fs = require("fs");

const { connectRabbitMQ } = require("./config/rabbitmq");
const startFollowersRPC = require("./services/followConsumer");

const app = express();

// Middleware
app.use(cors({ origin: "http://localhost:5173", credentials: true }));
app.use(bodyParser.json());

// Import model và route
const User = require("./models/userModel");
const userRoutes = require("./routes/userRoutes");
app.use("/api/users", userRoutes);

const Follow = require("./models/followModel");
const followRoutes = require("./routes/followRoutes");
app.use("/api/follow", followRoutes);

const adminRoutes=require("./routes/adminRoutes");
app.use("/api/userAdmin",adminRoutes);

// Kết nối MongoDB
mongoose.connect(process.env.MONGO_URI)
  .then(() => console.log("✅ User service connected to MongoDB"))
  .catch(err => console.error("❌ MongoDB connection error:", err));

const db = mongoose.connection;

// Khi DB mở → import dữ liệu nếu rỗng
db.once("open", async () => {
  try {
    const count = await User.countDocuments();
    if (count === 0 && fs.existsSync("./users.json")) {
      const data = JSON.parse(fs.readFileSync("./users.json", "utf-8"));
      await User.insertMany(data);
      console.log("✅ Imported users.json vào MongoDB");
    }

    const followCount = await Follow.countDocuments();
    if (followCount === 0 && fs.existsSync("./follows.json")) {
      const data = JSON.parse(fs.readFileSync("./follows.json", "utf-8"));
      const followDocs = data.map(f => ({ _id: `${f.from}-${f.to}`, from: f.from, to: f.to }));
      await Follow.insertMany(followDocs);
      console.log("✅ Imported follows.json");
    }
    // ======= AUTO BUILD countUser nếu rỗng =======
const CountUser = require("./models/countUserModel");
const countUserDocs = await CountUser.countDocuments();

if (countUserDocs === 0) {
  console.log("⚠️ countUser trống → bắt đầu đếm lại từ users...");

  const pipelineUser = [
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
  ];

  const userGrouped = await User.aggregate(pipelineUser);

  if (userGrouped.length === 0) {
    console.log("ℹ️ users collection trống, bỏ qua tạo countUser.");
  } else {
    const docs = userGrouped.map(g => ({
      year: g._id.year,
      month: g._id.month,
      count: g.count
    }));

    await CountUser.insertMany(docs);
    console.log("✅ Đã tạo dữ liệu thống kê countUser từ users!");
  }
}
    // ======= AUTO BUILD countFollow nếu rỗng =======
    const CountFollow = require("./models/countFollowModel");
    const countFollowDocs = await CountFollow.countDocuments();

    if (countFollowDocs === 0) {
      console.log("⚠️ countFollow trống → bắt đầu đếm lại từ follows...");

      // Gom theo tháng/năm
      const pipeline = [
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
      ];

      const grouped = await Follow.aggregate(pipeline);

      if (grouped.length === 0) {
        console.log("ℹ️ follows collection trống, bỏ qua việc tạo countFollow.");
      } else {
        const docs = grouped.map(g => ({
          year: g._id.year,
          month: g._id.month,
          count: g.count
        }));

        await CountFollow.insertMany(docs);
        console.log("✅ Đã tạo dữ liệu thống kê countFollow từ follows!");
      }
    }
  } catch (err) {
    console.error("❌ Lỗi import dữ liệu:", err);
  }
});

// ==== KHỞI TẠO RABBITMQ VÀ RPC ====
async function startServices() {
  await connectRabbitMQ();       // đảm bảo kết nối xong
  await startFollowersRPC();     // đảm bảo channel sẵn sàng
}

startServices();

// Chạy server
const PORT = process.env.PORT || 3002;
app.listen(PORT, () => console.log(`🚀 User service chạy ở http://localhost:${PORT}`));
