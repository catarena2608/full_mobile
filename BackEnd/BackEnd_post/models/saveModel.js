const mongoose = require("mongoose");

const saveSchema = new mongoose.Schema(
  {
    _id: {
      type: String, // userID_postID
    },
    userID: {
      type: String,
      required: true,
      index: true,
    },
    postID: {
      type: String,
      required: true,
      index: true,
    },
  },
  {
    timestamps: true,
    versionKey: false,
  }
);

// Tự tạo composite key
saveSchema.pre("validate", function (next) {
  if (!this._id) {
    this._id = `${this.userID}_${this.postID}`;
  }
  next();
});

// Phòng trường hợp _id bị vô hiệu hóa
saveSchema.index({ userID: 1, postID: 1 }, { unique: true });

module.exports = mongoose.model("Save", saveSchema,"saves");
