import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Clock, Users } from 'lucide-react';
import api from '../services/api';

const RecipeDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [recipe, setRecipe] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchRecipeDetail();
    }, [id]);

    const fetchRecipeDetail = async () => {
        try {
            setLoading(true);
            const response = await api.get(`/api/recipe/${id}`);
            if (response.data.success) {
                setRecipe(response.data.recipe);
            } else {
                setError('Failed to load recipe details');
            }
        } catch (err) {
            console.error("Error fetching recipe:", err);
            if (err.response) {
                if (err.response.status === 404) {
                    setError('Recipe not found (404)');
                } else if (err.response.status === 502) {
                    setError('Gateway Error (502) - Service unavailable');
                } else {
                    setError(`Server Error (${err.response.status}): ${err.response.data.message || err.message}`);
                }
            } else if (err.request) {
                setError('No response from server. Check your network connection.');
            } else {
                setError('Error setting up request: ' + err.message);
            }
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <div className="p-8 text-center">Loading...</div>;
    if (error) return (
        <div className="p-8 text-center">
            <div className="text-red-500 mb-4">{error}</div>
            <button onClick={() => navigate(-1)} className="text-blue-600 hover:underline">Go back</button>
        </div>
    );
    if (!recipe) return <div className="p-8 text-center">Recipe not found</div>;

    return (
        <div className="space-y-4 max-w-5xl mx-auto">
            <button
                onClick={() => navigate(-1)}
                className="flex items-center text-gray-600 hover:text-gray-900 transition-colors"
            >
                <ArrowLeft size={18} className="mr-2" />
                Quay lại
            </button>

            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
                <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
                    {/* Left Column: Image & Basic Info (4 cols) */}
                    <div className="md:col-span-4 space-y-4">
                        {recipe.thumbnail && (
                            <img
                                src={recipe.thumbnail}
                                alt={recipe.name}
                                className="w-full h-48 object-cover rounded-lg shadow-sm"
                            />
                        )}

                        <div className="flex justify-between text-sm text-gray-500 bg-gray-50 p-3 rounded-lg border border-gray-100">
                            <span className="flex items-center"><Clock size={16} className="mr-2" /> {recipe.time}</span>
                            <span className="flex items-center"><Users size={16} className="mr-2" /> {recipe.ration} người</span>
                        </div>

                        <div>
                            <h3 className="font-semibold text-gray-800 mb-2 text-base">Nguyên Liệu</h3>
                            <div className="bg-gray-50 p-3 rounded-lg border border-gray-100 max-h-96 overflow-y-auto">
                                <ul className="space-y-1">
                                    {recipe.ingredients && Object.entries(recipe.ingredients).map(([key, value]) => (
                                        <li key={key} className="flex justify-between items-center text-xs">
                                            <span className="font-medium text-gray-700">{key}</span>
                                            <span className="text-gray-500">{value}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </div>
                    </div>

                    {/* Right Column: Title, Description, Guide (8 cols) */}
                    <div className="md:col-span-8 space-y-4">
                        <div>
                            <h1 className="text-2xl font-bold text-gray-900 mb-2">{recipe.name}</h1>
                            <p className="text-gray-600 text-sm leading-relaxed">{recipe.description}</p>
                        </div>

                        <div className="border-t border-gray-100 pt-4">
                            <h3 className="font-semibold text-gray-800 mb-3 text-base">Hướng Dẫn Thực Hiện</h3>
                            <div className="space-y-4">
                                {recipe.guide && recipe.guide.map((step, index) => (
                                    <div key={index} className="flex space-x-3">
                                        <div className="flex-shrink-0 w-6 h-6 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center font-bold text-xs shadow-sm mt-0.5">
                                            {index + 1}
                                        </div>
                                        <div className="flex-1">
                                            <p className="text-gray-700 text-sm mb-2">{step.text || step}</p>
                                            {step.media && step.media.length > 0 && (
                                                <div className="flex gap-2 overflow-x-auto pb-1">
                                                    {step.media.map((m, i) => (
                                                        <img key={i} src={m} alt="" className="h-16 w-16 object-cover rounded-md shadow-sm border border-gray-100" />
                                                    ))}
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default RecipeDetail;
