import { useEffect, useState } from "react";
import { Card, Avatar, List, Tag } from "antd";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import "./HomeContent.scss";

const HomeContent = () => {
  const [users, setUsers] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    const getAllUsers = async () => {
      try {
        const res = await axios.get("http://localhost:3000/api/users", {
          withCredentials: true, // gửi cookie HTTP-only
        });
        if (res.status === 200 && res.data) {
          setUsers(res.data);
        }
      } catch (err) {
        console.error(err);
      }
    };
    getAllUsers();
  }, []);

  const handleClick = async (id) => {
    const res = await axios.get(`http://localhost:3000/api/users/${id}`, {
          withCredentials: true, // gửi cookie HTTP-only
        });
    localStorage.setItem("otherProfile", JSON.stringify(res.data));
    navigate(`/profile/${id}`);
  };

  return (
    <div className="home-content">
      <h2 className="home-title">Danh sách người dùng</h2>

      <List
        grid={{ gutter: 16, column: 3 }}
        dataSource={users}
        renderItem={(user) => (
          <List.Item>
            <Card
              onClick={() => handleClick(user._id)}
              className="user-card"
              hoverable
              cover={
                <img
                  alt={user.name}
                  src={user.avatar || "https://via.placeholder.com/300"}
                  className="user-avatar"
                />
              }
            >
              <Card.Meta
                title={user.name}
                description={
                  <div className="user-info">
                    <p>{`User Name: ${user.user_name}`}</p>
                    <p>Bài viết: {user.numPosts ?? 0}</p>
                    <p>
                      Theo dõi: {user.numFollowed ?? 0} | Đang theo dõi:{" "}
                      {user.numFollowing ?? 0}
                    </p>

                    <div className="user-tags">
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
                  </div>
                }
              />
            </Card>
          </List.Item>
        )}
      />
    </div>
  );
};

export default HomeContent;
