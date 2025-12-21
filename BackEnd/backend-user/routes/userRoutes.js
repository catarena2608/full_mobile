const express = require("express");
const router = express.Router();
const User = require("../models/userModel");
const Follow = require("../models/followModel");
const removeAccent = require("../utils/removeAccent");
const auth = require("../utils/checkHeader");
const { getChannel } = require("../config/rabbitmq");
const STATS_QUEUE = process.env.RABBITMQ_STATS_QUEUE || "stats_queue";

/* ------------------------- SEARCH (ĐỂ LÊN ĐẦU) ------------------------- */
router.get("/search", async (req, res) => {
  try {
    const { q, field } = req.query;
    if (!q) {
      return res.status(400).json({ message: "Thiếu q để search" });
    }

    const searchKey = removeAccent(q);
    const regex = new RegExp(searchKey.split("").join(".*"), "i");

    let query = {};
    let typeArr = [];

    if (field === "name") {
      query = { name_noAccent: regex };
      typeArr = ["name"];
    } else if (field === "user_name") {
      query = { user_name_noAccent: regex };
      typeArr = ["user_name"];
    } else {
      query = {
        $or: [{ name_noAccent: regex }, { user_name_noAccent: regex }],
      };
      typeArr = ["name", "user_name"];
    }

    const users = await User.find(query)
      .select("id user_name name avatar numPosts numFollowed numFollowing tags")
      .limit(20)
      .lean();

    /* ---------------------- BUILD TARGET ARRAY ---------------------- */
    let target = [];

    if (typeArr.includes("name")) {
      target = target.concat(
        users.map(u => u.name).slice(0, 5)
      );
    }

    if (typeArr.includes("user_name")) {
      target = target.concat(
        users.map(u => u.user_name).slice(0, 5)
      );
    }

    target = [...new Set(target)]; // tránh trùng
    target = target.slice(0, 10); // chặn tối đa 10 item

    // 🔔 Push event vào RabbitMQ
    const channel = getChannel(STATS_QUEUE);

    const payload = {
      keyword: q,
      target,
      type: typeArr
    };

    if (!channel) {
      console.error("❌ Không thể gửi RabbitMQ: Channel STATS chưa sẵn sàng!");
    } else {
      console.log("📤 Sending SEARCH STATS event to RabbitMQ:", payload);
      channel.sendToQueue(
        STATS_QUEUE,
        Buffer.from(JSON.stringify(payload)),
        { persistent: true }
      );
    }

    return res.json({ success: true, total: users.length, users });
  } catch (err) {
    console.error("❌ Lỗi search:", err);
    res.status(500).json({ message: err.message });
  }
});

/* ------------------------- SEARCH BY TAG ------------------------- */
router.get("/tag", async (req, res) => {
  try {
    const { q, after } = req.query;
    const limit = 20;

    const query = {};
    if (after) query.createdAt = { $lt: new Date(after) };

    let typeArr = ["tag"];
    let targetTags = [];

    if (req.query.tag) {
      // tag multiple
      let tags = req.query.tag;
      if (!Array.isArray(tags)) {
        tags = tags.split(",").map(t => t.trim());
      }

      const regexTags = tags.map(t => new RegExp(removeAccent(t), "i"));
      query.tags = { $all: regexTags };

      targetTags = tags.slice(0, 10); // max 10 tag gửi lên
    } else if (q && q.trim() !== "") {
      const keyword = removeAccent(q.trim());
      query.tags = { $regex: keyword, $options: "i" };

      targetTags = [q]; // từ khóa tag
    }

    const users = await User.find(query)
      .select("id user_name name avatar numPosts numFollowed numFollowing tags")
      .sort({ createdAt: -1 })
      .limit(limit)
      .lean();

    const nextCursor =
      users.length > 0 ? users[users.length - 1].createdAt : null;

    // 🔔 Push event vào RabbitMQ
    const channel = getChannel(STATS_QUEUE);

    const payload = {
      keyword: q || "",
      target: targetTags.slice(0, 10),
      type: typeArr
    };

    if (!channel) {
      console.error("❌ Không thể gửi RabbitMQ: Channel STATS chưa sẵn sàng!");
    } else {
      console.log("📤 Sending TAG SEARCH STATS event to RabbitMQ:", payload);
      channel.sendToQueue(
        STATS_QUEUE,
        Buffer.from(JSON.stringify(payload)),
        { persistent: true }
      );
    }
  
    return res.json({
      success: true,
      total: users.length,
      users,
      nextCursor,
    });
  } catch (err) {
    console.error("❌ Lỗi search tag user:", err);
    return res.status(500).json({ message: err.message });
  }
});


/* ------------------------- GET ALL USERS ------------------------- */
router.get("/", auth, async (req, res) => {
  try {
    const currentUserId = req.user.userID;

    const users = await User.find()
      .select("id user_name name avatar numPosts numFollowed numFollowing tags")
      .lean();

    const followList = await Follow.find({ from: currentUserId })
      .select("to")
      .lean();

    const followingSet = new Set(followList.map(f => f.to));

    users.forEach(u => {
      u.meFollow = followingSet.has(u._id.toString());
    });

    res.json({ success: true, users });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/* ------------------------- GET USER BY ID ------------------------ */
router.get("/:id", auth, async (req, res) => {
  try {
    const targetId = req.params.id;
    const currentUserId = req.user.userID;

    const user = await User.findById(targetId)
      .select("id user_name name avatar coverImage numPosts numFollowed numFollowing tags link preference")
      .lean();

    if (!user) {
      return res.status(404).json({ message: "Không tìm thấy user" });
    }

    const exists = await Follow.findById(`${currentUserId}-${targetId}`);
    user.meFollow = !!exists;

    res.json({ success: true, user });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
});

/* ------------------------- EDIT PROFILE (SELF) -------------------- */
router.patch("/profile", auth, async (req, res) => {
  try {
    const userID = req.user.userID;
    const { name, tags, email, avatar, link, preference } = req.body;

    const linkArray = Array.isArray(link)
      ? link
      : typeof link === "string"
      ? link.split("\n").map(l => l.trim()).filter(l => l)
      : [];

    const updateData = {
      name,
      name_noAccent: removeAccent(name),
      tags,
      email,
      avatar,
      link: linkArray,
      preference: {
        allergy: preference?.allergy || [],
        illness: preference?.illness || [],
        diet: preference?.diet ? [preference.diet] : ["Bình thường"],
      },
    };

    const updatedUser = await User.findByIdAndUpdate(
      userID,
      updateData,
      { new: true }
    ).select(
      "_id name email avatar coverImage numPosts numFollowed numFollowing tags link preference"
    );

    if (!updatedUser)
      return res.status(404).json({ message: "Không tìm thấy user để cập nhật" });

    res.json({
      success: true,
      message: "Cập nhật hồ sơ thành công",
      user: updatedUser,
    });
  } catch (err) {
    console.error("❌ Lỗi update profile:", err);
    res.status(500).json({ message: err.message });
  }
});

// 🧩 POST /api/user/report
router.post("/report", async (req, res) => {
  try {
    const author = req.header("x-user-id");
    const { target, content } = req.body;

    if (!author || !target) {
      return res.status(400).json({
        success: false,
        message: "Thiếu author (header x-user-id) hoặc target (userID)"
      });
    }

    // 🚫 0️⃣ Không cho tự report chính mình (không cần query DB)
    if (author.toString() === target.toString()) {
      return res.status(400).json({
        success: false,
        message: "Bạn không thể report chính mình."
      });
    }

    // 1️⃣ Kiểm tra user có tồn tại không
    const existUser = await User.findById(target)
      .select("_id user_name name")
      .lean();

    if (!existUser) {
      return res.status(404).json({
        success: false,
        message: "User không tồn tại hoặc đã bị xóa!"
      });
    }
    
    // 2️⃣ Payload gửi sang Static Service
    const payload = {
      author,
      reportedUser: target,
      type: "user",
      target,
      content: content || ""
    };

    // 3️⃣ Gửi message vào RabbitMQ
    const channel = getChannel(STATS_QUEUE);
    if (!channel) {
      console.error("❌ Không thể gửi RabbitMQ: Channel chưa có!");
      return res.status(500).json({
        success: false,
        message: "Không thể gửi message vào RabbitMQ"
      });
    }

    console.log("📤 Sending USER REPORT to RabbitMQ:", payload);
    channel.sendToQueue(
      STATS_QUEUE,
      Buffer.from(JSON.stringify(payload)),
      { persistent: true }
    );

    return res.json({
      success: true,
      message: "Report user đã được gửi vào hàng đợi"
    });

  } catch (err) {
    console.error("❌ Lỗi report user:", err);
    return res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
