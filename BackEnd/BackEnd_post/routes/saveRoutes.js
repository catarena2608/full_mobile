const express = require("express");
const router = express.Router();
const Save = require("../models/saveModel");

// ⭐ SAVE bài viết
router.post("/", async (req, res) => {
  try {
    const userID = req.header("x-user-id");
    const { postID } = req.body;

    if (!userID || !postID) {
      return res.status(400).json({
        success: false,
        message: "Thiếu userID (header) hoặc postID (body)",
      });
    }

    const saveID = `${userID}_${postID}`;

    const existed = await Save.findById(saveID);
    if (existed) {
      return res.json({ success: true, message: "Đã save trước đó" });
    }

    await Save.create({ userID, postID });

    res.json({
      success: true,
      message: "Đã lưu bài viết ⭐",
    });
  } catch (err) {
    console.error("❌ Lỗi save:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

// ⭐ UNSAVE
router.delete("/", async (req, res) => {
  try {
    const userID = req.header("x-user-id");
    const { postID } = req.body;

    if (!userID || !postID) {
      return res.status(400).json({
        success: false,
        message: "Thiếu userID (header) hoặc postID (body)",
      });
    }

    const saveID = `${userID}_${postID}`;
    const deleted = await Save.findByIdAndDelete(saveID);

    if (!deleted) {
      return res.json({
        success: true,
        message: "Chưa từng save bài này",
      });
    }

    res.json({
      success: true,
      message: "Đã bỏ lưu ⭐",
    });
  } catch (err) {
    console.error("❌ Lỗi unsave:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
