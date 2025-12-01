import { useEffect, useState } from "react";
import { Button, Avatar, Tag, message, Spin } from "antd";
import { HeartOutlined, HeartFilled, MessageOutlined } from "@ant-design/icons";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { setPost } from "../../../features/postData/postDataSlice";

import "./Newsfeed.scss";

const Newsfeed = () => {
  const [posts, setPosts] = useState([]);
  const [nextCursor, setNextCursor] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const userData = useSelector((state) => state.userData);
  const dispatch = useDispatch();

  const API_POST = "http://localhost:3000/api/post";
  const API_USER = "http://localhost:3000/api/users";
  const API_LIKE = "http://localhost:3000/api/post/like";

  const handleClickPost = (post) => {
    if (post.type === "Recipe") {
    navigate(`/recipeDetail/${post._id}`); // dùng _id của post để truyền
    return;
  }
    dispatch(setPost(post)); // 🧠 lưu post vào Redux
    navigate(`/normalDetail/${post._id}`); // không cần state nữa
  };
  // 🧩 Lấy thông tin user (theo danh sách userID)
  const fetchUsers = async (userIDs) => {
    try {
      const uniqueIDs = [...new Set(userIDs)];
      const responses = await Promise.all(
        uniqueIDs.map((id) =>
          axios
            .get(`${API_USER}/${id}`, { withCredentials: true })
            .then((res) => res.data)
            .catch(() => null)
        )
      );

      // 🔗 Tạo map { userID: userData }
      const userMap = {};
      responses.forEach((user) => {
        if (user && user._id) userMap[user._id] = user;
      });

      return userMap;
    } catch (err) {
      console.error("Lỗi khi fetch user:", err);
      message.error("Không thể tải thông tin người dùng");
      return {};
    }
  };

  // 📦 Lấy danh sách bài viết + nối user
  const fetchPosts = async (after = null, append = false) => {
    try {
      setLoading(true);
      const res = await axios.get(API_POST, {
        params: { after, userID: userData.id },
        withCredentials: true,
      });

      if (!res.data.success) {
        message.warning("Không thể tải bài viết");
        return;
      }

      const posts = res.data.posts || [];
      setNextCursor(res.data.nextCursor || null);

      // ⚙️ Lấy danh sách userID
      const userIDs = posts.map((p) => p.userID);
      const userMap = await fetchUsers(userIDs);

      // 🧩 Gắn user vào từng post
      const enrichedPosts = posts.map((p) => ({
        ...p,
        user: userMap[p.userID] || null,
      }));

      // ⚙️ Cập nhật state
      if (append) setPosts((prev) => [...prev, ...enrichedPosts]);
      else setPosts(enrichedPosts);
    } catch (err) {
      console.error("Lỗi khi tải bài viết:", err);
      message.error("Lỗi khi tải bài viết");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPosts();
  }, []);

  // ❤️ Xử lý khi nhấn "Thích"
  const handleLike = async (postID) => {
    // Cập nhật UI ngay lập tức (optimistic update)
    setPosts((prev) =>
      prev.map((p) =>
        p._id === postID ? { ...p, meLike: true, like: p.like + 1 } : p
      )
    );

    try {
      const res = await axios.post(
        API_LIKE,
        { userID: userData.id, postID },
        { withCredentials: true }
      );
      if (!res.data.success) throw new Error(res.data.message);
    } catch (err) {
      console.error("❌ Lỗi khi like:", err);
      message.error("Không thể like bài viết");
      // 🔄 Rollback UI nếu lỗi
      setPosts((prev) =>
        prev.map((p) =>
          p._id === postID ? { ...p, meLike: false, like: Math.max(p.like - 1, 0) } : p
        )
      );
    }
  };

  // 💔 Xử lý khi bỏ "Thích"
  const handleUnlike = async (postID) => {
    // Cập nhật UI ngay lập tức
    setPosts((prev) =>
      prev.map((p) =>
        p._id === postID ? { ...p, meLike: false, like: Math.max(p.like - 1, 0) } : p
      )
    );

    try {
      const res = await axios.delete(API_LIKE, {
        data: { userID: userData.id, postID },
        withCredentials: true,
      });
      if (!res.data.success) throw new Error(res.data.message);
    } catch (err) {
      console.error("❌ Lỗi khi unlike:", err);
      message.error("Không thể bỏ like bài viết");
      // 🔄 Rollback nếu lỗi
      setPosts((prev) =>
        prev.map((p) =>
          p._id === postID ? { ...p, meLike: true, like: p.like + 1 } : p
        )
      );
    }
  };
  return (
    <div className="newsfeed-container">
      {loading && posts.length === 0 ? (
        <div className="loading">
          <Spin size="large" />
        </div>
      ) : (
        posts.map((post) => (
          <div key={post._id} className="post-card" onClick={() => handleClickPost(post)}
            style={{ cursor: "pointer" }}>
            <div className="post-header">
              <div className="user-info">
                <Avatar
                  size={48}
                  src={
                    post.user?.avatar ||
                    `https://api.dicebear.com/9.x/initials/svg?seed=${post.user?.name || post.userID
                    }`
                  }
                />
                <div className="user-text">
                  <h4>{post.user?.name || "Ẩn danh"}</h4>
                  <span className="user-location">
                    {post.location?.name || ""}
                  </span>
                </div>
              </div>
              <Button className="follow-btn">Follow</Button>
            </div>

            <div className="post-media">
              {post.media?.[0] && (
                <img src={post.media[0]} alt="post media" loading="lazy" />
              )}
            </div>

            <div className="post-info">
              <div className="tags">
                {post.type && (
                  <Tag color="orange" className="type-tag">
                    {post.type}
                  </Tag>
                )}
                {post.tag?.slice(0, 3).map((t) => (
                  <Tag key={t} color="blue">
                    #{t}
                  </Tag>
                ))}
              </div>

              <p className="caption">{post.caption}</p>

              <div className="actions">
                <div className="icon-group">
                  {post.meLike ? (
                    <Button
                      type="text"
                      icon={<HeartFilled style={{ color: "red", fontSize: 20 }} />}
                      onClick={() => handleUnlike(post._id)}
                    />
                  ) : (
                    <Button
                      type="text"
                      icon={<HeartOutlined style={{ fontSize: 20 }} />}
                      onClick={() => handleLike(post._id)}
                    />
                  )}
                  <span>{post.like}</span>
                </div>

                <div className="icon-group">
                  <MessageOutlined /> {Math.floor(Math.random() * 5)}
                </div>
              </div>

              <div className="user-caption">
                <b>{post.user?.name || "Ẩn danh"}</b> {post.caption}
              </div>
            </div>
          </div>
        ))
      )}

      {nextCursor && !loading && (
        <div className="load-more">
          <Button type="primary" onClick={() => fetchPosts(nextCursor, true)}>
            Xem thêm
          </Button>
        </div>
      )}
    </div>
  );
};

export default Newsfeed;
