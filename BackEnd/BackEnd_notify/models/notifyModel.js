const mongoose = require("mongoose");
const {
  v4: uuidv4
} = require("uuid");

const notifySchema = new mongoose.Schema({
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

  type: {
    type: String,
    enum: ["like", "comment", "new_post", "follow", "reply", "remove_post", "remove_comment",
      "warn_user", "warn_post", "warn_comment",
      "ban_user", "ban_post", "ban_comment"
    ],
    required: true
  },

  targetID: {
    type: String,
    default: null
  }, // postID / commentID

  isRead: {
    type: Boolean,
    default: false
  }
}, {
  timestamps: true
});

notifySchema.set("id", false);

module.exports = mongoose.model("Notify", notifySchema, "notifies");