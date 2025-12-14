const jwt = require("jsonwebtoken");
const Notify = require("../models/notifyModel");
const Block = require("../models/blockModel");

let io = null;
const onlineUsers = new Map();      // userID -> Set(socketId)
const userBlockCache = new Map();   // userID -> Set(actorID)

function initSocketIO(server) {
  io = require("socket.io")(server, {
    cors: { origin: "*" }
  });

  // 🔐 JWT auth
  io.use((socket, next) => {
    const token = socket.handshake.auth?.token;
    if (!token) return next(new Error("NO_TOKEN"));

    try {
      const decoded = jwt.verify(token, process.env.MyJWT_SECRET);
      socket.userID = decoded.id;
      next();
    } catch {
      next(new Error("INVALID_TOKEN"));
    }
  });

  io.on("connection", async socket => {
    const userID = socket.userID;

    // register socket
    if (!onlineUsers.has(userID)) onlineUsers.set(userID, new Set());
    onlineUsers.get(userID).add(socket.id);

    console.log(`🔌 User ${userID} connected (${socket.id})`);

    // 🔥 load block list → cache
    const blocks = await Block.find({ userID }).select("actorID -_id");
    userBlockCache.set(
      userID,
      new Set(blocks.map(b => b.actorID))
    );

    // 🔥 load notifications (filtered)
    const notifies = await Notify.find({
      userID,
      isRead: false,
      actorID: { $nin: [...userBlockCache.get(userID)] }
    }).sort({ createdAt: -1 });

    socket.emit("init_notifications", notifies);

    // ✅ MARK AS READ
    socket.on("mark_as_read", async ({ notifyIDs }) => {
      if (!Array.isArray(notifyIDs) || notifyIDs.length === 0) return;

      await Notify.updateMany(
        { _id: { $in: notifyIDs }, userID },
        { $set: { isRead: true } }
      );

      socket.emit("mark_as_read_success", { notifyIDs });
    });

    // 🚫 BLOCK
    socket.on("block_user", async ({ actorID }) => {
      if (!actorID || actorID === userID) return;

      await Block.updateOne(
        { userID, actorID },
        { $setOnInsert: { userID, actorID } },
        { upsert: true }
      );

      userBlockCache.get(userID).add(actorID);
      socket.emit("block_success", { actorID });
    });

    // ♻️ UNBLOCK
    socket.on("unblock_user", async ({ actorID }) => {
      await Block.deleteOne({ userID, actorID });
      userBlockCache.get(userID)?.delete(actorID);
      socket.emit("unblock_success", { actorID });
    });

    socket.on("disconnect", () => {
      const set = onlineUsers.get(userID);
      set.delete(socket.id);

      if (set.size === 0) {
        onlineUsers.delete(userID);
        userBlockCache.delete(userID);
      }

      console.log(`❌ User ${userID} disconnected (${socket.id})`);
    });
  });
}

function emitToUser(userID, payload) {
  if (!io) return;

  const actorID = payload.actorID;
  if (!actorID) return;

  if (Array.isArray(userID)) {
    userID.forEach(uid => emitToUser(uid, payload));
    return;
  }

  // 🔐 BLOCK CHECK (REALTIME)
  const blocked = userBlockCache.get(userID);
  if (blocked?.has(actorID)) return;

  const sockets = onlineUsers.get(userID);
  if (!sockets) return;

  sockets.forEach(sid => {
    io.to(sid).emit("notify", payload);
  });
}

module.exports = { initSocketIO, emitToUser };
