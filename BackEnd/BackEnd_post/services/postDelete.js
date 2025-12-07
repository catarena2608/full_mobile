const Post = require("../models/postModel");
const Like = require("../models/likeModel");
const Save = require("../models/saveModel");
const Comment = require("../models/commentModel");
const CountPost = require("../models/countPostModel");

async function deletePostCascade(postID) {
  // 1. Xóa Post
  const post = await Post.findByIdAndDelete(postID);
  if (!post) return null;

  const year = post.createdAt.getFullYear();
  const month = post.createdAt.getMonth() + 1;

  // 2. Xóa toàn bộ comments
  const { deletedCount: delC } = await Comment.deleteMany({ postID });

  // 3. Xóa likes
  const { deletedCount: delL } = await Like.deleteMany({ postID });

  // 4. Xóa saves
  const { deletedCount: delS } = await Save.deleteMany({ postID });

  // 5. Giảm thống kê post count tháng
  await CountPost.updateOne(
    { year, month },
    { $inc: { count: -1 } }
  );

  return {
    deletedPost: true,
    deletedComments: delC,
    deletedLikes: delL,
    deletedSaves: delS
  };
}

module.exports = { deletePostCascade };
