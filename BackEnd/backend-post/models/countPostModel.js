const mongoose = require("mongoose");
const { v4: uuidv4 } = require("uuid");

const countPostSchema = new mongoose.Schema({
  _id: { type: String, default: uuidv4 },
  count: { type: Number, required: true, default: 0 },
  month: { type: Number, required: true, min: 1, max: 12, index: true },
  year: { type: Number, required: true, index: true }
});

// Unique index để 1 tháng chỉ có 1 record
countPostSchema.index({ year: 1, month: 1 }, { unique: true });

countPostSchema.set("id", false);

module.exports = mongoose.model("countPost", countPostSchema, "countPost");
