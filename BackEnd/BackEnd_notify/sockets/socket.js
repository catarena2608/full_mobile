const jwt = require("jsonwebtoken");

let io = null;
const onlineUsers = new Map(); // userId -> Set(socketIds)

function initSocketIO(server) {
  io = require("socket.io")(server, {
    cors: { origin: "*" }
  });

  // 🔐 JWT middleware trước khi socket được accept
  io.use((socket, next) => {
    const token = socket.handshake.auth?.token;
    if (!token) return next(new Error("NO_TOKEN"));

    try {
      const decoded = jwt.verify(token, process.env.MyJWT_SECRET);
      socket.userID = decoded.id;
      next();
    } catch (err) {
      return next(new Error("INVALID_TOKEN"));
    }
  });

  io.on("connection", socket => {
    const userID = socket.userID;

    // đưa socket vào danh sách user
    if (!onlineUsers.has(userID)) onlineUsers.set(userID, new Set());
    onlineUsers.get(userID).add(socket.id);

    console.log(`🔌 User ${userID} connected with socket ${socket.id}`);

    socket.on("disconnect", () => {
      const userSockets = onlineUsers.get(userID);
      if (!userSockets) return;

      userSockets.delete(socket.id);

      if (userSockets.size === 0) {
        onlineUsers.delete(userID);
      }

      console.log(`❌ User ${userID} disconnected socket ${socket.id}`);
    });
  });
}

function emitToUser(userID, payload) {
  if (!io) return;

  // Nếu userID là array, emit cho từng người
  if (Array.isArray(userID)) {
    userID.forEach(uid => emitToUser(uid, payload));
    return;
  }

  const sockets = onlineUsers.get(userID);
  if (!sockets) return;

  sockets.forEach(sid => {
    io.to(sid).emit("notify", payload);
  });
}

module.exports = { initSocketIO, emitToUser };
