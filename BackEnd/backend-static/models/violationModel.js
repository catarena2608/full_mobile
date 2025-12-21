const mongoose = require("mongoose");
const { v4: uuidv4 } = require("uuid");

const violationSchema = new mongoose.Schema({
  _id: { type: String, default: uuidv4 },
  userID: { type: String, required: true },
  action: { 
    type: String, 
    enum: [ "warn", "ban"], 
    required: true 
  },
  type: { 
    type: String, 
    enum: ["post", "comment", "user", ""], 
    default:"" 
  },
  target: { type: String },        // postID / commentID / userID bị report
  reason: { type: String, default: "" },
  end: { type: Boolean, default: true },
  expiredAt: { type: Date },                 // thời gian hết ban
  createdAt: { type: Date, default: Date.now }
});
violationSchema.set("id", false);

module.exports = mongoose.model("Violate", violationSchema, "violation");
