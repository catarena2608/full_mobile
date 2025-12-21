const mongoose = require("mongoose");
const { v4: uuidv4 } = require("uuid");

const topSearchSchema = new mongoose.Schema({
  _id: { type: String, default: uuidv4 },
  type: {type: [String],required:true},
  target: { type: String, required: true },   // Keyword normalize
  count: { type: Number, default: 1 },

}, { timestamps: true });

topSearchSchema.set("id", false);

module.exports = mongoose.model("TopSearch", topSearchSchema, "topSearch");
