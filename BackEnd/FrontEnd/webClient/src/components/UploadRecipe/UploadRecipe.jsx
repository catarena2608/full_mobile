import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Form,
  Input,
  InputNumber,
  Button,
  Upload,
  Card,
  Space,
  Divider,
  Typography,
  Select,
  message,
  Modal,
} from "antd";
import { UploadOutlined, PlusOutlined, MenuOutlined } from "@ant-design/icons";
import { DragDropContext, Droppable, Draggable } from "@hello-pangea/dnd";
import { useSelector } from "react-redux";
import axios from "axios";

const { TextArea } = Input;
const { Title } = Typography;

const ingredientSections = [
  { key: "base", label: "Nguyên liệu nền" },
  { key: "comple", label: "Nguyên liệu đi kèm" },
  { key: "spice", label: "Gia vị" },
  { key: "other", label: "Khác" },
];

const UploadRecipe = () => {
  const [form] = Form.useForm();
  const [ingredients, setIngredients] = useState({
    base: [],
    comple: [],
    spice: [],
    other: [],
  });
  const [guides, setGuides] = useState([]);
  const [loading, setLoading] = useState(false);

  const userData = useSelector((state) => state.userData);
  const navigate = useNavigate();

  const handleAddIngredient = (section) => {
    setIngredients({
      ...ingredients,
      [section]: [...ingredients[section], { quantity: "", name: "" }],
    });
  };

  const handleChangeIngredient = (section, index, field, value) => {
    const updated = { ...ingredients };
    updated[section][index][field] = value;
    setIngredients(updated);
  };

  const handleDragEnd = (result, section) => {
    if (!result.destination) return;
    const items = Array.from(ingredients[section]);
    const [reordered] = items.splice(result.source.index, 1);
    items.splice(result.destination.index, 0, reordered);
    setIngredients({ ...ingredients, [section]: items });
  };

  const handleAddGuide = () => {
    setGuides([...guides, { step: guides.length + 1, content: "", media: [] }]);
  };

  const handleGuideChange = (index, field, value) => {
    const updated = [...guides];
    updated[index][field] = value;
    setGuides(updated);
  };

  const handleMediaUpload = (index, info) => {
    const updated = [...guides];
    updated[index].media = info.fileList.map((f) => f.originFileObj);
    setGuides(updated);
  };

  // ----------------- GỬI FORM -----------------
  const onFinish = async (values) => {
    if (!userData?.id) {
      message.error("Không tìm thấy thông tin người dùng!");
      return;
    }

    try {
      setLoading(true);

      // 🧩 1️⃣ Gọi API POST tạo bài viết trước để lấy postID
      const postForm = new FormData();
      postForm.append("userID", userData.id);
      postForm.append("type", "Recipe");
      postForm.append("caption", values.caption || "");

      if (values.thumbnail && values.thumbnail.length > 0)
        postForm.append("media", values.thumbnail[0].originFileObj);

      const postRes = await axios.post("http://localhost:3000/api/post/upload", postForm, {
        headers: { "Content-Type": "multipart/form-data" },
        withCredentials: true,
      });

      if (!postRes.data?.success) {
        message.error("Tạo bài viết thất bại!");
        return;
      }

      const postID = postRes.data?.postID || postRes.data?.post?._id;
      if (!postID) {
        message.error("Không nhận được postID từ server!");
        return;
      }

      // 🧩 2️⃣ Gọi API /api/recipe/upload
      const recipeForm = new FormData();
      recipeForm.append("userID", userData.id);
      recipeForm.append("postID", postID);
      recipeForm.append("caption", values.caption || "");
      recipeForm.append("name", values.name);
      recipeForm.append("description", values.description || "");
      recipeForm.append("ration", values.ration || 1);
      recipeForm.append("time", values.time || "");

      recipeForm.append(
        "tags",
        JSON.stringify(Array.isArray(values.tags) ? values.tags : [])
      );
      recipeForm.append("ingredients", JSON.stringify(ingredients));
      recipeForm.append("guide", JSON.stringify(guides));

      // Ảnh thumbnail
      if (values.thumbnail && values.thumbnail.length > 0)
        recipeForm.append("media", values.thumbnail[0].originFileObj);

      // Ảnh trong hướng dẫn
      guides.forEach((g) => {
        g.media?.forEach((file) => recipeForm.append("media", file));
      });

      const recipeRes = await axios.post(
        "http://localhost:3000/api/recipe/upload",
        recipeForm,
        {
          headers: { "Content-Type": "multipart/form-data" },
          withCredentials: true,
        }
      );

      if (recipeRes.data?.success) {
        Modal.success({
          title: "🎉 Đăng công thức thành công!",
          content: "Công thức và bài viết đã được lưu.",
          onOk: () => navigate("/"),
        });

        form.resetFields();
        setGuides([]);
        setIngredients({
          base: [],
          comple: [],
          spice: [],
          other: [],
        });
      } else {
        Modal.error({
          title: "❌ Đăng công thức thất bại",
          content: recipeRes.data?.message || "Đã có lỗi xảy ra",
        });
      }
    } catch (err) {
      console.error("❌ Lỗi khi upload recipe:", err);
      message.error("Không thể kết nối server");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card style={{ maxWidth: 700, margin: "auto" }}>
      <Title level={3}>Đăng công thức nấu ăn</Title>
      <Form layout="vertical" form={form} onFinish={onFinish}>
        <Form.Item
          label="Ảnh món ăn"
          name="thumbnail"
          valuePropName="fileList"
          getValueFromEvent={(e) => e?.fileList}
        >
          <Upload listType="picture-card" beforeUpload={() => false}>
            <UploadOutlined /> Upload
          </Upload>
        </Form.Item>

        <Form.Item label="Caption" name="caption">
          <Input placeholder="Chia sẻ đôi lời về món ăn..." />
        </Form.Item>

        <Form.Item
          label="Tên món"
          name="name"
          rules={[{ required: true, message: "Tên món là bắt buộc" }]}
        >
          <Input placeholder="Tên món ăn" />
        </Form.Item>

        <Form.Item name="tags" label="Hashtags">
          <Select mode="tags" placeholder="ví dụ: món ăn, mẹo nhỏ, chia sẻ" />
        </Form.Item>

        <Form.Item label="Mô tả" name="description">
          <TextArea rows={3} placeholder="Miêu tả món ăn..." />
        </Form.Item>

        <Form.Item label="Khẩu phần (người)" name="ration">
          <InputNumber min={1} />
        </Form.Item>

        <Divider />
        <Title level={4}>Nguyên liệu</Title>

        {ingredientSections.map(({ key, label }) => (
          <Card
            key={key}
            size="small"
            title={label}
            style={{ marginBottom: 16 }}
            extra={
              <Button
                type="dashed"
                size="small"
                onClick={() => handleAddIngredient(key)}
                icon={<PlusOutlined />}
              >
                Thêm
              </Button>
            }
          >
            <DragDropContext onDragEnd={(res) => handleDragEnd(res, key)}>
              <Droppable droppableId={key}>
                {(provided) => (
                  <div ref={provided.innerRef} {...provided.droppableProps}>
                    {ingredients[key].map((item, index) => (
                      <Draggable
                        key={index}
                        draggableId={`${key}-${index}`}
                        index={index}
                      >
                        {(provided) => (
                          <Space
                            ref={provided.innerRef}
                            {...provided.draggableProps}
                            {...provided.dragHandleProps}
                            style={{
                              display: "flex",
                              marginBottom: 8,
                              width: "100%",
                            }}
                          >
                            <MenuOutlined />
                            <Space.Compact style={{ width: "100%" }}>
                              <Input
                                placeholder="Số lượng"
                                value={item.quantity}
                                onChange={(e) =>
                                  handleChangeIngredient(
                                    key,
                                    index,
                                    "quantity",
                                    e.target.value
                                  )
                                }
                              />
                              <Input
                                placeholder="Tên nguyên liệu"
                                value={item.name}
                                onChange={(e) =>
                                  handleChangeIngredient(
                                    key,
                                    index,
                                    "name",
                                    e.target.value
                                  )
                                }
                              />
                            </Space.Compact>
                          </Space>
                        )}
                      </Draggable>
                    ))}
                    {provided.placeholder}
                  </div>
                )}
              </Droppable>
            </DragDropContext>
          </Card>
        ))}

        <Divider />
        <Title level={4}>Cách làm</Title>
        <Form.Item label="Thời gian nấu" name="time">
          <Input placeholder="Ví dụ: 1 tiếng 30 phút" />
        </Form.Item>

        {guides.map((step, index) => (
          <Card
            key={index}
            size="small"
            title={`Bước ${step.step}`}
            style={{ marginBottom: 12 }}
          >
            <TextArea
              placeholder="Mô tả bước này..."
              rows={2}
              value={step.content}
              onChange={(e) =>
                handleGuideChange(index, "content", e.target.value)
              }
            />
            <Upload
              listType="picture"
              multiple
              beforeUpload={() => false}
              onChange={(info) => handleMediaUpload(index, info)}
            >
              <Button icon={<UploadOutlined />}>Thêm ảnh / video</Button>
            </Upload>
          </Card>
        ))}

        <Button
          type="dashed"
          onClick={handleAddGuide}
          icon={<PlusOutlined />}
          block
        >
          Thêm bước
        </Button>

        <Divider />
        <Button
          type="primary"
          htmlType="submit"
          block
          loading={loading}
          disabled={loading}
        >
          {loading ? "Đang đăng..." : "Đăng công thức"}
        </Button>
      </Form>
    </Card>
  );
};

export default UploadRecipe;
