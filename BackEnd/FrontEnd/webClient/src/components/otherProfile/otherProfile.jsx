import { useEffect, useState } from "react";
import { Card, Avatar, Tag, Spin } from "antd";
import axios from "axios";
import { useParams } from "react-router-dom";
import "./otherProfile.scss";

const OtherProfile = () => {
  const { id } = useParams();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const localData = localStorage.getItem("otherProfile");
    if (localData) {
      const parsed = JSON.parse(localData);
      if (parsed._id === id) {
        setUser(parsed);
        setLoading(false);
        return;
      }
    }

    const fetchUser = async () => {
      try {
        const res = await axios.get(`http://localhost:3000/api/users/${id}`, {
          withCredentials: true, // 👈 Gửi cookie kèm request
        });

        setUser(res.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchUser();
  }, [id]);

  if (loading) {
    return (
      <div className="profile-loading">
        <Spin size="large" />
      </div>
    );
  }

  if (!user) {
    return <p>Không tìm thấy người dùng</p>;
  }

  return (
    <div className="other-profile">
      {/* Cover Image */}
      <div
        className="cover-image"
        style={{
          backgroundImage: `url(${user.coverImage || "https://via.placeholder.com/1200x400"})`,
        }}
      />

      {/* Avatar + Info */}
      <div className="profile-section">
        <Card className="profile-card">
          <Avatar
            size={128}
            src={user.avatar || "https://via.placeholder.com/150"}
            alt={user.name}
            className="profile-avatar"
          />
          <h2>{user.name}</h2>
          <p>{user.user_name}</p>

          <div className="stats">
            <p>Bài viết: {user.numPosts ?? 0}</p>
            <p>
              Người theo dõi: {user.numFollowed ?? 0} | Đang theo dõi:{" "}
              {user.numFollowing ?? 0}
            </p>
          </div>

          <div className="tags">
            {user.tags && user.tags.length > 0 ? (
              user.tags.map((tag) => (
                <Tag color="green" key={tag}>
                  {tag}
                </Tag>
              ))
            ) : (
              <Tag color="default">No Tag</Tag>
            )}
          </div>

          {user.link && user.link.length > 0 && (
            <div className="links">
              <h4>Liên kết:</h4>
              {user.link.map((l, i) => (
                <a href={l} key={i} target="_blank" rel="noreferrer">
                  {l}
                </a>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};

export default OtherProfile;
