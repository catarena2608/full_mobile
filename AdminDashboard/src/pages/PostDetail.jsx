import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import api from '../services/api';
import PostDetailContent from '../components/PostDetailContent';

const PostDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [post, setPost] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchPost = async () => {
            try {
                setLoading(true);
                const response = await api.get(`/api/postAdmin/${id}`);
                if (response.data.success) {
                    setPost(response.data.post);
                } else {
                    setError('Failed to fetch post');
                }
            } catch (err) {
                console.error("Error fetching post:", err);
                setError('Error connecting to server');
            } finally {
                setLoading(false);
            }
        };

        if (id) {
            fetchPost();
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

            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                <div className="p-6 border-b border-gray-100">
                    <h1 className="text-2xl font-bold text-gray-900">Chi Tiết Bài Viết</h1>
                </div>
                {post && <PostDetailContent post={post} />}
            </div>
        </div>
    );
};

export default PostDetail;
