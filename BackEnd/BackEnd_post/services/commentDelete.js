const Comment = require("../models/commentModel");
const Post = require("../models/postModel");

async function deleteCommentCascade(commentID) {
  const comment = await Comment.findById(commentID);
  if (!comment) return null;

  const postID = comment.postID;

  // 1. Xóa comment chính
  await Comment.findByIdAndDelete(commentID);

  // 2. Xóa reply cấp 1
  const level1 = await Comment.find({ parentID: commentID }).select("_id");
  const lvl1IDs = level1.map(c => c._id);

  await Comment.deleteMany({ parentID: commentID });

  // 3. Xóa reply cấp 2
  if (lvl1IDs.length > 0) {
    await Comment.deleteMany({
      parentID: { $in: lvl1IDs }
    });
  }

  // 4. Trừ comment count của Post
  await Post.findByIdAndUpdate(postID, {
    $inc: { comment: -1 }
  });

  return {
    deletedReplies: lvl1IDs.length,
    postID
  };
}

module.exports = { deleteCommentCascade };
