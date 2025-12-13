const mongoose = require("mongoose");
const {
  v4: uuidv4
} = require("uuid");

const blockSchema = new mongoose.Schema({
  _id: {
    type: String,
    default: uuidv4
  },

  userID: {
    type: String,
    required: true
  }, // người nhận notify
  actorID: {
    type: String,
    required: true
  }, // người gây ra action
});

blockSchema.set("id", false);

module.exports = mongoose.model("Block", blockSchema, "block");