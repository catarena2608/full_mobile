import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Avatar, Button, Tag, Spin, message, Input, Popconfirm } from "antd";
import { useSelector, useDispatch } from "react-redux";
import { clearPost } from "../../../features/postData/postDataSlice";
import axios from "axios";
import "./NormalDetail.scss";

const { TextArea } = Input;

const NormalDetail = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { postID } = useParams();
  const post = useSelector((state) => state.postData);
  const userData = useSelector((state) => state.userData); // ✅ lấy thông tin user hiện tại
  const [comments, setComments] = useState([]);
  const [userMap, setUserMap] = useState({});
  const [nextCursor, setNextCursor] = useState(null);
  const [loading, setLoading] = useState(false);
  const [localPost, setLocalPost] = useState(null);
  const [newComment, setNewComment] = useState(""); // ✅ nội dung bình luận mới
  const [replyTo, setReplyTo] = useState(null); // ✅ nếu reply thì lưu ID comment cha

  const API_POST = "http://localhost:3000/api/post";
  const API_COMMENT = "http://localhost:3000/api/post/comment";
  const API_USER = "http://localhost:3000/api/users";

  // 🧩 Lấy chi tiết post (nếu reload)
  const fetchPost = async () => {
    try {
      const res = await axios.get(`${API_POST}/${postID}`, {
        withCredentials: true,
      });
      if (res.data.success) return res.data.post;
      message.error("Không thể tải bài viết");
      return null;
    } catch (err) {
      console.error(err);
      message.error("Lỗi khi tải bài viết");
      return null;
    }
  };

  // 🧩 Lấy thông tin user (theo danh sách ID)
  const fetchUsers = async (userIDs) => {
    const uniqueIDs = [...new Set(userIDs)].filter((id) => !userMap[id]);
    if (uniqueIDs.length === 0) return;

    try {
      const responses = await Promise.all(
        uniqueIDs.map((id) =>
          axios
            .get(`${API_USER}/${id}`, { withCredentials: true })
            .then((res) => res.data)
            .catch(() => null)
        )
      );
      const newMap = {};
      responses.forEach((u) => {
        if (u && u._id) newMap[u._id] = u;
      });
      setUserMap((prev) => ({ ...prev, ...newMap }));
    } catch (err) {
      console.error("❌ Lỗi fetch user:", err);
    }
  };

  // 💬 Lấy danh sách comment (3 tầng)
  const fetchComments = async (after = null, append = false) => {
    try {
      setLoading(true);
      const res = await axios.get(`${API_COMMENT}/${postID}`, {
        params: { after },
        withCredentials: true,
      });
      if (!res.data.success)
        return message.warning("Không thể tải bình luận");

      const newComments = res.data.comments || [];
      setNextCursor(res.data.nextCursor || null);

      // ⚙️ Gom toàn bộ userID xuất hiện
      const collectUserIDs = (arr) => {
        let ids = [];
        for (const c of arr) {
          ids.push(c.userID);
          if (c.replies?.length) ids = ids.concat(collectUserIDs(c.replies));
        }
        return ids;
      };
      const allUserIDs = collectUserIDs(newComments);
      await fetchUsers(allUserIDs);

      if (append) setComments((prev) => [...prev, ...newComments]);
      else setComments(newComments);
    } catch (err) {
      console.error(err);
      message.error("Lỗi khi tải bình luận");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const init = async () => {
      if (!post) {
        const fetched = await fetchPost();
        setLocalPost(fetched);
      }
      fetchComments();
    };
    init();
    return () => {
      dispatch(clearPost());
    };
  }, [postID]);

  // 🧩 Gửi bình luận mới
  const handleAddComment = async () => {
    if (!newComment.trim()) return message.warning("Nhập nội dung bình luận!");
    try {
      const res = await axios.post(
        `${API_COMMENT}`,
        {
          userID: userData.id,
          postID,
          content: newComment.trim(),
          reply: replyTo, // nếu đang reply thì có ID
        },
        { withCredentials: true }
      );
      if (res.data.success) {
        message.success("Đã gửi bình luận 💬");
        setNewComment("");
        setReplyTo(null);
        fetchComments();
      }
    } catch (err) {
      console.error(err);
      message.error("Không thể gửi bình luận");
    }
  };

  // 🗑 Xóa bình luận (chỉ của chính mình)
  const handleDeleteComment = async (commentID, userID) => {
    if (userID !== userData.id)
      return message.warning("Bạn chỉ có thể xóa bình luận của mình!");

    try {
      const res = await axios.delete(`${API_COMMENT}/${commentID}`, {
        withCredentials: true,
      });
      if (res.data.success) {
        message.success("Đã xóa bình luận 🗑️");
        fetchComments();
      }
    } catch (err) {
      console.error(err);
      message.error("Không thể xóa bình luận");
    }
  };

  const activePost = post || localPost;
  if (!activePost) return <Spin size="large" />;

  // 🧱 Render replies (3 tầng)
  const renderReplies = (replies, depth = 1) => {
    if (!replies || replies.length === 0 || depth > 2) return null;
    return (
      <div className={`comment-level-${depth}`}>
        {replies.map((r) => {
          const user = userMap[r.userID];
          return (
            <div key={r._id} className="comment-item">
              <Avatar
                size={32}
                src={
                  user?.avatar ||
                  `https://api.dicebear.com/9.x/initials/svg?seed=${user?.name || "?"}`
                }
              />
              <b>{user?.name || "Ẩn danh"}</b>: {r.content}
              <div className="comment-actions">
                <Button size="small" type="link" onClick={() => setReplyTo(r._id)}>
                  Trả lời
                </Button>
                {r.userID === userData.id && (
                  <Popconfirm
                    title="Xóa bình luận?"
                    onConfirm={() => handleDeleteComment(r._id, r.userID)}
                    okText="Xóa"
                    cancelText="Hủy"
                  >
                    <Button size="small" type="link" danger>
                      Xóa
                    </Button>
                  </Popconfirm>
                )}
              </div>
              {renderReplies(r.replies, depth + 1)}
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div className="detail-container">
      {/* ====== Bài viết ====== */}
      <div className="post-card">
        <div className="post-header">
          <Avatar
            size={48}
            src={
              activePost.user?.avatar ||
              `https://api.dicebear.com/9.x/initials/svg?seed=${activePost.user?.name}`
            }
          />
          <div>
            <h4>{activePost.user?.name || "Ẩn danh"}</h4>
            <span>{activePost.location?.name || ""}</span>
          </div>
        </div>

        {activePost.media?.[0] && (
          <div className="post-media">
            <img src={activePost.media[0]} alt="post media" loading="lazy" />
          </div>
        )}

        <div className="post-info">
          <div className="tags">
            {activePost.type && <Tag color="orange">{activePost.type}</Tag>}
            {activePost.tag?.slice(0, 3).map((t) => (
              <Tag key={t} color="blue">
                #{t}
              </Tag>
            ))}
          </div>
          <p>{activePost.caption}</p>
        </div>
      </div>

      {/* ====== Bình luận ====== */}
      <div className="comments-section">
        <h3>Bình luận</h3>

        {/* ✏️ Form nhập bình luận */}
        <div className="add-comment">
          {replyTo && (
            <div className="replying-to">
              Đang trả lời bình luận{" "}
              <Button size="small" type="link" onClick={() => setReplyTo(null)}>
                ❌ Hủy
              </Button>
            </div>
          )}
          <TextArea
            rows={2}
            placeholder="Viết bình luận..."
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
          />
          <Button type="primary" onClick={handleAddComment} style={{ marginTop: 8 }}>
            Gửi
          </Button>
        </div>

        {comments.length === 0 && !loading && <p>Chưa có bình luận nào.</p>}
        {loading && <Spin />}

        {comments.map((c) => {
          const user = userMap[c.userID];
          return (
            <div key={c._id} className="comment-item">
              <Avatar
                size={36}
                src={
                  user?.avatar ||
                  `https://api.dicebear.com/9.x/initials/svg?seed=${user?.name || "?"}`
                }
              />
              <b>{user?.name || "Ẩn danh"}</b>: {c.content}
              <div className="comment-actions">
                <Button size="small" type="link" onClick={() => setReplyTo(c._id)}>
                  Trả lời
                </Button>
                {c.userID === userData.id && (
                  <Popconfirm
                    title="Xóa bình luận?"
                    onConfirm={() => handleDeleteComment(c._id, c.userID)}
                    okText="Xóa"
                    cancelText="Hủy"
                  >
                    <Button size="small" type="link" danger>
                      Xóa
                    </Button>
                  </Popconfirm>
                )}
              </div>
              {renderReplies(c.replies, 1)}
            </div>
          );
        })}

        {nextCursor && !loading && (
          <Button
            onClick={() => fetchComments(nextCursor, true)}
            type="default"
            style={{ marginTop: 10 }}
          >
            Xem thêm bình luận
          </Button>
        )}
      </div>
    </div>
  );
};

export default NormalDetail;
