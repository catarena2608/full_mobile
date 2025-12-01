const mongoose = require("mongoose");
const { v4: uuidv4 } = require("uuid");

const adminSchema = new mongoose.Schema({
  _id: { type: String, default: uuidv4 },

  username: { type: String, required: true },
  password: { type: String, required: true },
  action: { type: Boolean, default: false }
});

adminSchema.set("id", false);

module.exports = mongoose.model("Admin", adminSchema, "admins");
