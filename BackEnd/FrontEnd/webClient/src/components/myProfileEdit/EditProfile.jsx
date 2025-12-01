import { useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { App } from "antd";

import {
  Form,
  Input,
  Button,
  Upload,
  Avatar,
  Typography,
  Divider,
  Select,
  Radio,
  Modal,
  Card,
} from "antd";
import {
  UploadOutlined,
  SaveOutlined,
  ArrowLeftOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { setUser, clearUser } from "../../../features/userData/userDataSlice";
import "./EditProfile.scss";

const { Title } = Typography;
const { TextArea } = Input;

const allergyOptions = [
  "Các loại hạt",
  "Ngũ cốc chứa gluten",
  "Trứng",
  "Đậu phộng",
  "Cá",
  "Đậu nành",
  "Sữa",
  "Mè",
  "Cần tây",
  "Mù tạt",
  "Động vật có vỏ",
  "Động vật thân mềm",
  "Cây họ đậu lupin",
  "Lưu huỳnh dioxide và sulfite",
];

const illnessOptions = [
  "Tiểu đường",
  "Mỡ máu/ Mỡ gan",
  "Cao huyết áp",
  "Xương khớp",
  "Thiếu máu",
  "Gan/ Thận yếu",
  "Béo phì",
  "Dạ dày/ Tiêu hóa",
  "Hen suyễn",
  "Gout",
];

const dietOptions = [
  { label: "Bình thường", value: "Bình thường" },
  { label: "Giảm cân", value: "Giảm cân" },
  { label: "Chay", value: "Chay" },
  { label: "High-protein", value: "High-protein" },
  { label: "Thuần chay", value: "Thuần chay" },
];

const EditProfile = () => {
    const { modal } = App.useApp(); // <-- Lấy modal từ context
  const userData = useSelector((state) => state.userData);
  const dispatch = useDispatch();
  const [formProfile] = Form.useForm();
  const [formPreference] = Form.useForm();
  const [avatarPreview, setAvatarPreview] = useState(userData?.avatar || "");
  const navigate = useNavigate();

  if (!userData) return <div>Không tìm thấy người dùng</div>;

  const handleAvatarChange = (info) => {
    const file = info.file.originFileObj;
    if (file) {
      const reader = new FileReader();
      reader.onload = () => setAvatarPreview(reader.result);
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = async () => {
    try {
      const profileValues = await formProfile.validateFields();
      const preferenceValues = await formPreference.validateFields();

      const payload = {
        ...profileValues,
        avatar: avatarPreview,
        preference: preferenceValues,
      };

      const res = await axios.post(
  `http://localhost:3000/api/users/editProfile/${userData.id}`,
  payload, // <-- dữ liệu gửi lên
  { withCredentials: true } // <-- cấu hình, cookie sẽ tự gửi
);

      const updatedUser = res.data.user;

      modal.success({
  title: "Cập nhật thành công",
  content: "Hồ sơ của bạn đã được cập nhật!",
  onOk: () => {
    dispatch(clearUser());
    dispatch(setUser(updatedUser));
    navigate("/");
  },
});

    } catch (err) {
      console.error(err);
      modal.error({
  title: "Lỗi cập nhật hồ sơ",
  content:
    err.response?.data?.message || "Đã xảy ra lỗi khi lưu thông tin!",
  onOk: () => navigate("/"),
});

    }
  };

  return (
    <div className="edit-profile-container">
      <Card className="edit-profile-card" bordered={false}>
        <Button
          type="link"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(-1)}
          style={{ marginBottom: 10 }}
        >
          Quay lại
        </Button>

        <Title level={3}>Chỉnh sửa hồ sơ</Title>
        <Divider />

        {/* ============ PROFILE SECTION ============ */}
        <Title level={4}>Thông tin cá nhân</Title>
        <Form
          form={formProfile}
          layout="vertical"
          initialValues={{
            name: userData.name,
            tags: userData.tags?.join("\n") || "",
            email: userData.email,
            link: userData.link?.join("\n") || "",
          }}
        >
          <Form.Item label="Ảnh đại diện" name="avatar">
            <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
              <Avatar
                size={100}
                src={avatarPreview}
                style={{ border: "2px solid #ccc" }}
              />
              <Upload
                showUploadList={false}
                beforeUpload={() => false}
                onChange={handleAvatarChange}
              >
                <Button icon={<UploadOutlined />}>Chọn ảnh</Button>
              </Upload>
            </div>
          </Form.Item>

          <Form.Item
            label="Tên hiển thị"
            name="name"
            rules={[{ required: true, message: "Nhập tên" }]}
          >
            <Input placeholder="Nhập tên" />
          </Form.Item>

          <Form.Item label="Tags (mỗi dòng 1 tag)" name="tags">
            <TextArea rows={2} placeholder="Ví dụ: Foodie, Home Chef" />
          </Form.Item>

          <Form.Item
            label="Email"
            name="email"
            rules={[{ type: "email", message: "Email không hợp lệ" }]}
          >
            <Input placeholder="Nhập email" />
          </Form.Item>

          <Form.Item label="Liên kết (mỗi dòng 1 link)" name="link">
            <TextArea rows={3} placeholder="Ví dụ: https://facebook.com/tenban" />
          </Form.Item>
        </Form>

        <Divider />

        {/* ============ PREFERENCE SECTION ============ */}
        <Title level={4}>Sở thích & Hạn chế</Title>
        <Form
          form={formPreference}
          layout="vertical"
          initialValues={{
            allergy: userData.preference?.allergy || [],
            illness: userData.preference?.illness || [],
            diet: userData.preference?.diet?.[0] || "Bình thường",
          }}
        >
          <Form.Item label="Dị ứng" name="allergy">
            <Select
              mode="multiple"
              allowClear
              placeholder="Chọn dị ứng"
              options={allergyOptions.map((a) => ({ label: a, value: a }))}
            />
          </Form.Item>

          <Form.Item label="Bệnh lý" name="illness">
            <Select
              mode="multiple"
              allowClear
              placeholder="Chọn bệnh lý"
              options={illnessOptions.map((i) => ({ label: i, value: i }))}
            />
          </Form.Item>

          <Form.Item label="Chế độ ăn" name="diet">
            <Radio.Group
              options={dietOptions}
              optionType="button"
              buttonStyle="solid"
            />
          </Form.Item>
        </Form>

        <Divider />
        <Button
          type="primary"
          icon={<SaveOutlined />}
          onClick={handleSubmit}
          size="large"
        >
          Lưu thay đổi
        </Button>
      </Card>
    </div>
  );
};

export default EditProfile;
