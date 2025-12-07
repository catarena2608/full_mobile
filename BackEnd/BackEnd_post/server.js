const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");
const bodyParser = require("body-parser");
const fs = require("fs");
const cookieParser = require("cookie-parser");
require("dotenv").config();
const {
  connectRabbitMQ
} = require("./config/rabbitmq");
const normalizeDates= require("./utils/normalize");

const postRoutes = require("./routes/postRoutes");
const saveRoutes = require("./routes/saveRoutes");
const likeRoutes = require("./routes/likeRoutes");
const commentRoutes = require("./routes/commentRoutes");
const adminRoutes=require("./routes/adminRoutes");

const CountPost = require("./models/countPostModel");
const CountLike = require("./models/countLikeModel");

const Post = require("./models/postModel");
const Save = require("./models/saveModel");
const Like = require("./models/likeModel");
const Comment = require("./models/commentModel");

const startConsumer = require("./services/consumer");

const app = express();

app.use(cors({
  origin: "http://localhost:5173",
  credentials: true,
}));
app.use(bodyParser.json());
app.use(cookieParser());

app.use("/api/post", postRoutes);
app.use("/api/save", saveRoutes);
app.use("/api/like", likeRoutes);
app.use("/api/comment", commentRoutes);
app.use("/api/postAdmin", adminRoutes);

mongoose.connect(process.env.MONGO_URI)
  .then(async () => {
    console.log("✅ Post service connected to MongoDB");

    // -------------------
    // Import Posts
    // -------------------
    const postFile = "./backups/postdb.posts.json";
    if (fs.existsSync(postFile)) {
      const rawData = fs.readFileSync(postFile);
      let posts = JSON.parse(rawData);
      posts = normalizeDates(posts);
      const count = await Post.countDocuments();
      if (count === 0 && posts.length > 0) {
        await Post.insertMany(posts);
        console.log(`📦 Imported ${posts.length} posts from JSON`);
      } else {
        console.log("⚠️ Posts collection already has data, skip import");
      }
    } else {
      console.log("⚠️ postdb.posts.json not found, skip import");
    }

    // -------------------
    // Import Likes
    // -------------------
    const likeFile = "./backups/postdb.like.json";
    if (fs.existsSync(likeFile)) {
      const rawData = fs.readFileSync(likeFile);
      let likes = JSON.parse(rawData);
      likes = normalizeDates(likes);
      const count = await Like.countDocuments();
      if (count === 0 && likes.length > 0) {
        await Like.insertMany(likes);
        console.log(`💖 Imported ${likes.length} like entries from JSON`);
      } else {
        console.log("⚠️ Likes collection already has data, skip import");
      }
    } else {
      console.log("⚠️ postdb.like.json not found, skip import");
    }

    // =========================
    // 🧮 REBUILD countPost
    // =========================
    const countPostExists = await CountPost.countDocuments();
    if (countPostExists === 0) {
      console.log("📌 countPost empty → rebuilding from posts...");

      const postStats = await Post.aggregate([{
        $group: {
          _id: {
            year: {
              $year: "$createdAt"
            },
            month: {
              $month: "$createdAt"
            }
          },
          count: {
            $sum: 1
          }
        }
      }]);

      if (postStats.length > 0) {
        const docs = postStats.map(p => ({
          year: p._id.year,
          month: p._id.month,
          count: p.count
        }));

        await CountPost.insertMany(docs);
        console.log(`📊 Rebuilt countPost (${docs.length} months)`);
      } else {
        console.log("⚠️ No posts found → skip building countPost");
      }
    } else {
      console.log("✔ countPost already has data → skip rebuild");
    }


    // =========================
    // 💖 REBUILD countLike
    // =========================
    const countLikeExists = await CountLike.countDocuments();
    if (countLikeExists === 0) {
      console.log("📌 countLike empty → rebuilding from likes...");

      const likeStats = await Like.aggregate([{
        $group: {
          _id: {
            year: {
              $year: "$createdAt"
            },
            month: {
              $month: "$createdAt"
            }
          },
          count: {
            $sum: 1
          }
        }
      }]);

      if (likeStats.length > 0) {
        const docs = likeStats.map(l => ({
          year: l._id.year,
          month: l._id.month,
          count: l.count
        }));

        await CountLike.insertMany(docs);
        console.log(`💗 Rebuilt countLike (${docs.length} months)`);
      } else {
        console.log("⚠️ No likes found → skip building countLike");
      }
    } else {
      console.log("✔ countLike already has data → skip rebuild");
    }

    // ------------------- IMPORT COMMENTS -------------------
    const commentFile = "./backups/postdb.comment.json";
    if (fs.existsSync(commentFile)) {
      let comments = JSON.parse(fs.readFileSync(commentFile));
      comments = normalizeDates(comments);
      const count = await Comment.countDocuments();
      if (count === 0 && comments.length > 0) {
        await Comment.insertMany(comments);
        console.log(`💬 Imported ${comments.length} comments`);
      } else console.log("⚠️ Comments collection already has data, skip import");
    } else {
      console.log("⚠️ postdb.comment.json not found, skip import");
    }
    // ------------------- IMPORT SAVES -------------------
    const saveFile = "./backups/postdb.save.json";

    if (fs.existsSync(saveFile)) {
      const rawData = fs.readFileSync(saveFile);
      let saves = JSON.parse(rawData);
      saves = normalizeDates(saves);
      const count = await Save.countDocuments();

      if (count === 0 && saves.length > 0) {
        console.log("📥 Preparing saves import...");

        // ⭐ Không cần tự tạo _id, createdAt, updatedAt
        // → Mongoose sẽ tự generate composite key và timestamps
        const savesToInsert = saves.map(s => ({
          userID: s.userID,
          postID: s.postID
        }));

        await Save.insertMany(savesToInsert);
        console.log(`⭐ Imported ${saves.length} save entries`);
      } else {
        console.log("⚠️ Saves collection already has data, skip import");
      }
    } else {
      console.log("⚠️ postdb.save.json not found, skip import");
    }

    await connectRabbitMQ();
    console.log("🐰 Connected to RabbitMQ.");
    await startConsumer();
    // -------------------
    // Start server
    // -------------------
    app.listen(process.env.PORT || 4001, () =>
      console.log(`🚀 Post service running on port ${process.env.PORT || 4001}`)
    );
  })
  .catch(err => console.error("❌ MongoDB error:", err));