module.exports = function auth(req, res, next) {
  const userId = req.headers["x-user-id"];

  if (!userId) {
    return res.status(401).json({ message: "Thiếu thông tin user từ Gateway" });
  }

  // Gắn vào req.user
  req.user = { userID: userId };

  next();
};
