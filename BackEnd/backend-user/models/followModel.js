const mongoose = require("mongoose");

const FollowSchema = new mongoose.Schema({
  _id: {
    type: String,
    required: true
  },
  from: {
    type: String,
    required: true,
    index: true
  },
  to: {
    type: String,
    required: true,
    index: true
  }
}, {
  timestamps: true
});

// Auto-generate _id (from-to)
FollowSchema.pre("validate", function(next) {
  if (!this._id) {
    this._id = `${this.from}-${this.to}`;
  }
  next();
});

module.exports = mongoose.model("Follow", FollowSchema,"follows");
