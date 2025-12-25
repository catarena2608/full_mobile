const express = require("express");
const http = require("http");
const cors = require("cors");

const connectDB = require("./config/db");
const { connectRabbitMQ } = require("./config/rabbitmq");
const startNotifyConsumer = require("./services/notifyConsumer");
const { initSocketIO } = require("./sockets/socket");

const app = express();
app.use(cors({
  origin: process.env.FRONTEND_URL || "http://localhost:5173",
  credentials: true,
}));
app.use(express.json());

const server = http.createServer(app);

// Start
(async () => {
  try {
    console.log("🚀 Starting Notify Service...");
    
    // 1️⃣ Connect DB
    await connectDB();
    console.log("✅ Step 1: MongoDB connected");
    
    // 2️⃣ Connect RabbitMQ
    await connectRabbitMQ();
    console.log("✅ Step 2: RabbitMQ connected");
    
    // 3️⃣ Wait a bit for channels to be ready
    await new Promise(resolve => setTimeout(resolve, 1000));
    console.log("✅ Step 3: Waited for channels");
    
    // 4️⃣ Init Socket.IO
    initSocketIO(server);
    console.log("✅ Step 4: Socket.IO initialized");
    
    // 5️⃣ Start Consumer
    startNotifyConsumer();
    console.log("✅ Step 5: Consumer started");
    
    // 6️⃣ Start HTTP Server
    const PORT = process.env.PORT || 6001;
    server.listen(PORT, () => {
      console.log(`✅ Step 6: Notify Service running on port ${PORT}`);
      console.log("🎉 All services started successfully!");
    });
    
  } catch (err) {
    console.error("❌ Failed to start Notify Service:", err);
    process.exit(1);
  }
})();