const jwt = require("jsonwebtoken");

module.exports = (requireAction = false) => {
  return (req, res, next) => {
    const token = req.cookies.token;
    if (!token) return res.status(401).json({ message: "Thiếu token admin" });

    try {
      const admin = jwt.verify(token, process.env.MyJWT_SECRET);

      req.admin = {
        id: admin.id, //dùng id
        username: admin.username,
        action: admin.action //dùng quyền
      };

      if (requireAction && !admin.action) {
        return res.status(403).json({ message: "Admin không đủ quyền" });
      }

      next();
    } catch (err) {
      res.status(401).json({ message: `Token không hợp lệ: ${err.message}` });
    }
  };
};
