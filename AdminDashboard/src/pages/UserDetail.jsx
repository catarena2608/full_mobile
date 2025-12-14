import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import api from '../services/api';

const UserDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchUser = async () => {
            try {
                setLoading(true);
                const response = await api.get(`/api/userAdmin/${id}`);
                if (response.data.success) {
                    setUser(response.data.user);
                } else {
                    setError('Failed to fetch user');
                }
            } catch (err) {
                console.error("Error fetching user:", err);
                setError('Error connecting to server');
            } finally {
                setLoading(false);
            }
        };

        if (id) {
            fetchUser();
        }
    }, [id]);

    if (loading) return (
        <div className="flex justify-center items-center min-h-screen">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
    );

    if (error) return (
        <div className="p-8 text-center">
            <div className="text-red-500 mb-4">{error}</div>
            <button
                onClick={() => navigate(-1)}
                className="text-blue-600 hover:text-blue-800 font-medium"
            >
                Quay lại
            </button>
        </div>
    );

    return (
        <div className="max-w-4xl mx-auto py-8 px-4">
            <div className="mb-6">
                <button
                    onClick={() => navigate(-1)}
                    className="flex items-center text-gray-600 hover:text-gray-900 transition-colors"
                >
                    <ArrowLeft size={20} className="mr-2" />
                    Quay lại
                </button>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden p-8">
                <div className="flex items-center space-x-6 mb-8">
                    <div className="w-24 h-24 rounded-full bg-gray-200 overflow-hidden ring-4 ring-gray-50">
                        {user.avatar ? (
                            <img src={user.avatar} alt={user.name} className="w-full h-full object-cover" />
                        ) : (
                            <div className="w-full h-full flex items-center justify-center text-3xl font-bold text-gray-400">
                                {user.name?.charAt(0)}
                            </div>
                        )}
                    </div>
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900">{user.name}</h1>
                        <p className="text-lg text-gray-500">@{user.user_name}</p>
                        <p className="text-gray-400">{user.email}</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                    <div className="bg-blue-50 p-6 rounded-xl text-center">
                        <p className="text-3xl font-bold text-blue-600 mb-1">{user.numPosts || 0}</p>
                        <p className="text-sm text-gray-500 uppercase tracking-wide">Bài Viết</p>
                    </div>
                    <div className="bg-blue-50 p-6 rounded-xl text-center">
                        <p className="text-3xl font-bold text-blue-600 mb-1">{user.numFollowed || 0}</p>
                        <p className="text-sm text-gray-500 uppercase tracking-wide">Người Theo Dõi</p>
                    </div>
                    <div className="bg-blue-50 p-6 rounded-xl text-center">
                        <p className="text-3xl font-bold text-blue-600 mb-1">{user.numFollowing || 0}</p>
                        <p className="text-sm text-gray-500 uppercase tracking-wide">Đang Theo Dõi</p>
                    </div>
                </div>

                <div className="space-y-6">
                    <div>
                        <h3 className="text-lg font-bold text-gray-900 mb-3">Vai trò</h3>
                        <div className="flex flex-wrap gap-2">
                            {user.tags && user.tags.length > 0 ? (
                                user.tags.map((tag, i) => (
                                    <span key={i} className="px-3 py-1 bg-purple-100 text-purple-700 rounded-full text-sm font-medium">
                                        {tag}
                                    </span>
                                ))
                            ) : (
                                <span className="text-gray-500 italic">Người dùng thường</span>
                            )}
                        </div>
                    </div>

                    <div>
                        <h3 className="text-lg font-bold text-gray-900 mb-3">Sở Thích & Dị Ứng</h3>
                        <div className="flex flex-wrap gap-2">
                            {user.preference?.diet?.map((tag, i) => (
                                <span key={i} className="px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">{tag}</span>
                            ))}
                            {user.preference?.allergy?.map((tag, i) => (
                                <span key={i} className="px-3 py-1 bg-red-100 text-red-700 rounded-full text-sm font-medium">{tag}</span>
                            ))}
                            {(!user.preference?.diet?.length && !user.preference?.allergy?.length) && (
                                <span className="text-gray-500 italic">Không có thông tin</span>
                            )}
                        </div>
                    </div>

                    <div>
                        <h3 className="text-lg font-bold text-gray-900 mb-3">Thông Tin Khác</h3>
                        <div className="grid grid-cols-2 gap-4 text-sm">
                            <div>
                                <span className="text-gray-500">Ngày tham gia:</span>
                                <span className="ml-2 text-gray-900">{user.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN') : 'Unknown'}</span>
                            </div>
                            <div>
                                <span className="text-gray-500">ID:</span>
                                <span className="ml-2 text-gray-900 font-mono">{user._id}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default UserDetail;
