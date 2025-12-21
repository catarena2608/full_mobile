const mongoose = require("mongoose");
const { v4: uuidv4 } = require("uuid");
const removeAccent = require("../utils/removeAccent");

const userSchema = new mongoose.Schema({
  _id: { type: String, default: uuidv4 },
  user_name: { type: String, required: true },
  user_name_noAccent: { type: String, index: true }, // 🔥 để search nhanh
  name: { type: String, required: true },
  name_noAccent: { type: String, index: true },
  email: { type: String, required: true },
  password: { type: String, required: true },
  avatar: { type: String, required: true },
  coverImage: { type: String, default: "" },
  numPosts: { type: Number, default: 0 },
  numFollowed: { type: Number, default: 0 },
  numFollowing: { type: Number, default: 0 },
  tags: { type: [String], default: [] },
  link: { type: [String], default: [] },
  preference: {
    allergy: { type: [String], default: [] },
    illness: { type: [String], default: [] },
    diet: { type: [String], default: [] },
  },
}, { timestamps: true });

// Disable ObjectId
userSchema.set("id", false);

// 🔥 Pre-save → Auto tạo noAccent fields
userSchema.pre("save", function (next) {
  if (this.isModified("name")) {
    this.name_noAccent = removeAccent(this.name);
  }
  if (this.isModified("user_name")) {
    this.user_name_noAccent = removeAccent(this.user_name);
  }
  next();
});

// 🔥 Pre-insertMany → chạy cho import file JSON
userSchema.pre("insertMany", function (next, docs) {
  docs.forEach(doc => {
    doc.name_noAccent = removeAccent(doc.name);
    doc.user_name_noAccent = removeAccent(doc.user_name);
  });
  next();
});

module.exports = mongoose.model("User", userSchema);
