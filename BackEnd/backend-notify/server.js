const express = require("express");
const http = require("http");
const cors = require("cors");

const connectDB = require("./config/db");
const { connectRabbitMQ } = require("./config/rabbitmq");
const startNotifyConsumer = require("./services/notifyConsumer");
const { initSocketIO } = require("./sockets/socket");

const app = express();
app.use(cors({
  origin: "http://localhost:5173",
  credentials: true,
}));
app.use(express.json());

const server = http.createServer(app);

// Start
(async () => {
  await connectDB();
  await connectRabbitMQ();

  initSocketIO(server);
  startNotifyConsumer();

  server.listen(process.env.PORT || 6001, () => console.log("Notify Service running on 6001"));
})();
