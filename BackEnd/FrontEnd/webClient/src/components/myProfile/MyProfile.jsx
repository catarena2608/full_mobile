import { useSelector } from "react-redux";
import { Card, Avatar, Tag, Divider, Typography, Button } from "antd";
import {
  MailOutlined,
  LinkOutlined,
  UserOutlined,
  HeartOutlined,
  EditOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import "./MyProfile.scss";

const { Title, Paragraph } = Typography;

const MyProfile = () => {
  const userData = useSelector((state) => state.userData);
  const navigate = useNavigate();

  if (!userData) {
    return <div className="profile-empty">Chưa có dữ liệu người dùng</div>;
  }

  return (
    <div className="profile-container">
      <Card className="profile-card" bordered={false}>
        <div className="profile-header">
          <div className="profile-header-left">
            <Avatar
              size={120}
              src={userData.avatar}
              icon={<UserOutlined />}
              className="profile-avatar"
            />
            <div className="profile-info">
              <Title level={3} className="profile-name">
                {userData.name}
              </Title>
              <Paragraph className="profile-email">
                <MailOutlined /> {userData.email}
              </Paragraph>
              <div className="profile-stats">
                <span>Bài viết: {userData.numPosts}</span>
                <span>Theo dõi: {userData.numFollowed}</span>
                <span>Đang theo dõi: {userData.numFollowing}</span>
              </div>
            </div>
          </div>

          {/* ✅ Nút Edit Profile */}
          <Button
            type="primary"
            icon={<EditOutlined />}
            onClick={() => navigate("/profileEdit")}
            className="edit-btn"
          >
            Chỉnh sửa hồ sơ
          </Button>
        </div>

        <Divider />

        {userData.tags?.length > 0 && (
          <div className="profile-section">
            <Title level={5}>Thẻ</Title>
            <div className="profile-tags">
              {userData.tags.map((tag) => (
                <Tag color="blue" key={tag}>
                  {tag}
                </Tag>
              ))}
            </div>
          </div>
        )}

        {userData.link?.length > 0 && (
          <div className="profile-section">
            <Title level={5}>Liên kết</Title>
            <div className="profile-links">
              {userData.link.map((l) => (
                <a
                  key={l}
                  href={l.startsWith("http") ? l : `https://${l}`}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <LinkOutlined /> {l}
                </a>
              ))}
            </div>
          </div>
        )}

        {userData.preference && (
          <div className="profile-section">
            <Title level={5}>
              <HeartOutlined /> Sở thích & Hạn chế
            </Title>

            <div className="profile-pref">
              {userData.preference.allergy?.length > 0 && (
                <p>
                  <strong>Dị ứng:</strong>{" "}
                  {userData.preference.allergy.join(", ")}
                </p>
              )}
              {userData.preference.illness?.length > 0 && (
                <p>
                  <strong>Bệnh lý:</strong>{" "}
                  {userData.preference.illness.join(", ")}
                </p>
              )}
              {userData.preference.diet?.length > 0 && (
                <p>
                  <strong>Chế độ ăn:</strong>{" "}
                  {userData.preference.diet.join(", ")}
                </p>
              )}
            </div>
          </div>
        )}
      </Card>
    </div>
  );
};

export default MyProfile;
