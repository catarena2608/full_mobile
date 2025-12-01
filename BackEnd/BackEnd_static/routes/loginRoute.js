const express = require("express");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");

const Admin = require("../models/adminModel");

const router = express.Router();

// ---------------------------
// POST /stat/login
// ---------------------------
router.post("/", async (req, res) => {
  try {
    const { username, password } = req.body;

    if (!username || !password)
      return res.status(400).json({ success: false, message: "Thiếu username hoặc password" });

    const admin = await Admin.findOne({ username });

    if (!admin)
      return res.status(401).json({ success: false, message: "Sai username hoặc password" });

    const isMatch = await bcrypt.compare(password, admin.password);

    if (!isMatch)
      return res.status(401).json({ success: false, message: "Sai username hoặc password" });

    // Tạo JWT có thêm ACTION
    const token = jwt.sign(
      {
        id: admin._id,
        username: admin.username,
        action: admin.action, // <---- QUAN TRỌNG
      },
      process.env.MyJWT_SECRET,
      { expiresIn: "1d" }
    );

    // Set cookie HTTP-only
    res.cookie("token", token, {
      httpOnly: true,
      secure: false,
      sameSite: "lax",
      maxAge: 24 * 60 * 60 * 1000,
    });

    res.json({ success: true, token });

  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
