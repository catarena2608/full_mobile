import React, { useState, useEffect } from 'react';
import { Search, Clock, Users } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const Recipes = () => {
    const navigate = useNavigate();
    const [recipes, setRecipes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [sortBy, setSortBy] = useState('newest');

    useEffect(() => {
        fetchRecipes();
    }, []);

    const fetchRecipes = async () => {
        try {
            setError(null);
            const response = await api.get('/api/recipe');
            if (response.data.success) {
                setRecipes(Array.isArray(response.data.recipes) ? response.data.recipes : []);
            } else {
                setError('API returned success: false');
            }
        } catch (error) {
            console.error('Error fetching recipes:', error);
            setError(error.message || 'Failed to fetch recipes');
            setRecipes([]);
        } finally {
            setLoading(false);
        }
    };

    const handleRowClick = (recipe) => {
        navigate(`/recipe/${recipe._id}`);
    };

    const filteredRecipes = recipes
        .filter(recipe =>
            (recipe.name && recipe.name.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (recipe.description && recipe.description.toLowerCase().includes(searchTerm.toLowerCase()))
        )
        .sort((a, b) => {
            if (sortBy === 'newest') return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
            if (sortBy === 'time') {
                // Assuming time is a string like "30 mins", simple string sort might not be enough but good for now
                return (parseInt(a.time) || 0) - (parseInt(b.time) || 0);
            }
            if (sortBy === 'ration') return (a.ration || 0) - (b.ration || 0);
            return 0;
        });

    if (loading) return <div className="text-center py-10">Loading...</div>;
    if (error) return <div className="text-center py-10 text-red-500">Error: {error}. <br />Please check your network connection or backend status.</div>;

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold text-gray-800">Quản Lý Công Thức</h2>
                <div className="flex space-x-3">
                    <select
                        value={sortBy}
                        onChange={(e) => setSortBy(e.target.value)}
                        className="border border-gray-200 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                    >
                        <option value="newest">Mới Nhất</option>
                        <option value="time">Thời Gian Nấu</option>
                        <option value="ration">Khẩu Phần</option>
                    </select>
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                        <input
                            type="text"
                            placeholder="Tìm kiếm công thức..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                </div>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                <table className="w-full text-left">
                    <thead className="bg-gray-50 border-b border-gray-100">
                        <tr>
                            <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Công Thức</th>
                            <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Mô Tả</th>
                            <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Thời Gian</th>
                            <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Khẩu Phần</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                        {filteredRecipes.map((recipe) => (
                            <tr
                                key={recipe._id}
                                onClick={() => handleRowClick(recipe)}
                                className="hover:bg-gray-50 transition-colors cursor-pointer"
                            >
                                <td className="px-6 py-4">
                                    <div className="flex items-center space-x-3">
                                        {recipe.thumbnail ? (
                                            <img src={recipe.thumbnail} alt="" className="w-12 h-12 rounded-lg object-cover" />
                                        ) : (
                                            <div className="w-12 h-12 rounded-lg bg-gray-200 flex items-center justify-center text-gray-400 text-xs">No Img</div>
                                        )}
                                        <p className="text-sm font-medium text-gray-900">{recipe.name}</p>
                                    </div>
                                </td>
                                <td className="px-6 py-4 text-sm text-gray-600 truncate max-w-xs">{recipe.description}</td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center text-sm text-gray-500">
                                        <Clock size={14} className="mr-1" /> {recipe.time}
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center text-sm text-gray-500 font-medium">
                                        <Users size={14} className="mr-1" /> {recipe.ration} người
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default Recipes;
