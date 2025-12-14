import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, User, FileText, Calendar } from 'lucide-react';
import api from '../services/api';

const CommentDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [comment, setComment] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchComment();
    }, [id]);

    const fetchComment = async () => {
        try {
            setLoading(true);
            const response = await api.get(`/api/postAdmin/comment/${id}`);
            if (response.data.success) {
                setComment(response.data.comment);
            } else {
                setError('Failed to load comment');
            }
        } catch (err) {
            console.error("Error fetching comment:", err);
            setError('Error connecting to server');
        } finally {
            setLoading(false);
        }
    };

    const handleViewPost = () => {
        if (comment?.postID) navigate(`/post/${comment.postID}`);
    };

    const handleViewUser = () => {
        if (comment?.userID) navigate(`/user/${comment.userID}`);
    };

    if (loading) return <div className="p-8 text-center">Loading...</div>;
    if (error) return (
        <div className="p-8 text-center">
            <div className="text-red-500 mb-4">{error}</div>
            <button onClick={() => navigate(-1)} className="text-blue-600 hover:underline">Go back</button>
        </div>
    );
    if (!comment) return <div className="p-8 text-center">Comment not found</div>;

    return (
        <div className="space-y-6 max-w-3xl mx-auto">
            <button
                onClick={() => navigate(-1)}
                className="flex items-center text-gray-600 hover:text-gray-900 transition-colors"
            >
                <ArrowLeft size={20} className="mr-2" />
                Quay lại
            </button>

            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-8">
                <h1 className="text-2xl font-bold text-gray-900 mb-6">Chi Tiết Bình Luận</h1>

                <div className="space-y-6">
                    <div className="bg-gray-50 p-6 rounded-xl border border-gray-100">
                        <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">Nội Dung</h3>
                        <p className="text-gray-900 text-lg leading-relaxed">{comment.content}</p>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="bg-white border border-gray-100 p-4 rounded-xl shadow-sm">
                            <h3 className="flex items-center text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2">
                                <User size={16} className="mr-2" /> Người Đăng
                            </h3>
                            <p className="font-mono text-gray-900 mb-2 truncate">{comment.userID}</p>
                            <button
                                onClick={handleViewUser}
                                className="text-sm text-blue-600 hover:underline font-medium"
                            >
                                Xem Hồ Sơ Người Dùng
                            </button>
                        </div>

                        <div className="bg-white border border-gray-100 p-4 rounded-xl shadow-sm">
                            <h3 className="flex items-center text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2">
                                <FileText size={16} className="mr-2" /> Bài Viết Gốc
                            </h3>
                            <p className="font-mono text-gray-900 mb-2 truncate">{comment.postID}</p>
                            <button
                                onClick={handleViewPost}
                                className="text-sm text-blue-600 hover:underline font-medium"
                            >
                                Xem Bài Viết
                            </button>
                        </div>
                    </div>

                    <div className="flex items-center text-sm text-gray-500 bg-gray-50 px-4 py-2 rounded-lg inline-block">
                        <Calendar size={16} className="mr-2" />
                        Được tạo lúc: {new Date(comment.createdAt).toLocaleString('vi-VN')}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CommentDetail;
