import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Avatar, Tag, Spin, message } from "antd";
import axios from "axios";
import "./RecipeDetail.scss";

const RecipeDetail = () => {
  const { postID } = useParams();
  const [recipe, setRecipe] = useState(null);
  const [loading, setLoading] = useState(false);

  const API_RECIPE = "http://localhost:3000/api/recipe";

  useEffect(() => {
    const fetchRecipe = async () => {
      try {
        setLoading(true);
        const res = await axios.get(`${API_RECIPE}/${postID}`, { withCredentials: true });
        if (res.data.success) setRecipe(res.data.recipe);
        else message.error(res.data.message);
      } catch (err) {
        console.error(err);
        message.error("Lỗi khi tải recipe");
      } finally {
        setLoading(false);
      }
    };

    fetchRecipe();
  }, [postID]);

  if (loading || !recipe) return <Spin size="large" style={{ marginTop: 50 }} />;

  return (
    <div className="recipe-detail-container">
      <h2>{recipe.name}</h2>
      {recipe.thumbnail && <img src={recipe.thumbnail} alt={recipe.name} className="recipe-thumbnail" />}

      <p className="caption">{recipe.caption}</p>
      <p className="description">{recipe.description}</p>

      <div className="recipe-meta">
        <span>Số khẩu phần: {recipe.ration}</span>
        <span>Thời gian: {recipe.time}</span>
      </div>

      <div className="ingredients">
        <h3>Nguyên liệu</h3>
        {["base", "comple", "spice", "other"].map((group) => (
          <div key={group}>
            {recipe.ingredients[group]?.length > 0 && (
              <>
                <h4>{group}</h4>
                <ul>
                  {recipe.ingredients[group].map((i, idx) => (
                    <li key={idx}>{i.quantity} {i.name}</li>
                  ))}
                </ul>
              </>
            )}
          </div>
        ))}
      </div>

      <div className="guide">
        <h3>Hướng dẫn</h3>
        {recipe.guide.map((step, idx) => (
          <div key={idx} className="step">
            <h4>Bước {step.step}</h4>
            <p>{step.content}</p>
            {step.media?.map((url, i) => (
              <img key={i} src={url} alt={`step-${idx}`} className="step-media" />
            ))}
          </div>
        ))}
      </div>

      <div className="tags">
        {recipe.tags?.map((t) => (
          <Tag key={t} color="blue">#{t}</Tag>
        ))}
      </div>
    </div>
  );
};

export default RecipeDetail;
