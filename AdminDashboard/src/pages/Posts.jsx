import React, { useState, useEffect } from 'react';
import { Search, MessageSquare, Heart } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const Posts = () => {
    const navigate = useNavigate();
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [filterType, setFilterType] = useState('all');
    const [sortBy, setSortBy] = useState('newest');

    useEffect(() => {
        fetchPosts();
    }, []);

    const fetchPosts = async () => {
        try {
            setError(null);
            const response = await api.get('/api/postAdmin?limit=50', {
                headers: { 'x-user-id': 'admin-view' }
            });
            if (response.data.success) {
                const rawPosts = Array.isArray(response.data.posts) ? response.data.posts : [];

                // Fetch authors for these posts
                const userIds = [...new Set(rawPosts.map(p => p.userID))].filter(id => id);
                let userMap = {};

                if (userIds.length > 0) {
                    try {
                        const userRes = await api.get('/api/userAdmin', {
                            params: { ids: userIds.join(','), limit: userIds.length }
                        });
                        if (userRes.data.success) {
                            userRes.data.users.forEach(u => {
                                userMap[u._id] = u;
                            });
                        }
                    } catch (err) {
                        console.warn("Could not fetch authors:", err);
                    }
                }

                // Map posts with author info
                const mappedPosts = rawPosts.map(post => ({
                    ...post,
                    author: userMap[post.userID] || { name: 'Unknown', avatar: null }
                }));

                setPosts(mappedPosts);
            } else {
                setError('API returned success: false');
            }
        } catch (error) {
            console.error('Error fetching posts:', error);
            setError(error.message || 'Failed to fetch posts');
            setPosts([]);
        } finally {
            setLoading(false);
        }
    };

    const handleRowClick = (post) => {
        navigate(`/post/${post._id}`);
    };

    const filteredPosts = posts
        .filter(post => {
            const matchesSearch = (post.caption && post.caption.toLowerCase().includes(searchTerm.toLowerCase())) ||
                (post.location?.name && post.location.name.toLowerCase().includes(searchTerm.toLowerCase()));
            const matchesType = filterType === 'all' || post.type === filterType;
            return matchesSearch && matchesType;
        })
        .sort((a, b) => {
            if (sortBy === 'newest') return new Date(b.createdAt) - new Date(a.createdAt);
            if (sortBy === 'mostLikes') return (b.like || 0) - (a.like || 0);
            if (sortBy === 'mostComments') return (b.comment || 0) - (a.comment || 0);
            return 0;
        });

    if (loading) return <div className="text-center py-10">Loading...</div>;
    if (error) return <div className="text-center py-10 text-red-500">Error: {error}. <br />Please check your network connection or backend status.</div>;

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold text-gray-800">Quản Lý Bài Viết</h2>
                <div className="flex space-x-3">
                    <select
                        value={filterType}
                        onChange={(e) => setFilterType(e.target.value)}
                        className="border border-gray-200 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                    >
                        <option value="all">Tất Cả Loại</option>
                        <option value="Recipe">Công Thức</option>
                        <option value="Rate">Đánh Giá</option>
                        <option value="Tip">Mẹo</option>
                        <option value="Moment">Khoảnh Khắc</option>
                    </select>
                    <select
                        value={sortBy}
                        onChange={(e) => setSortBy(e.target.value)}
                        className="border border-gray-200 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                    >
                        <option value="newest">Mới Nhất</option>
                        <option value="mostLikes">Nhiều Like Nhất</option>
                        <option value="mostComments">Nhiều Bình Luận Nhất</option>
                    </select>
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                        <input
                            type="text"
                            placeholder="Tìm kiếm bài viết..."
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
                            <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Nội Dung</th>
                            <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Người Đăng</th>
                            <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Loại</th>
                            <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Thống Kê</th>
                            <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Vị Trí</th>
                            <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Ngày</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                        {filteredPosts.map((post) => (
                            <tr
                                key={post._id}
                                onClick={() => handleRowClick(post)}
                                className="hover:bg-gray-50 transition-colors cursor-pointer"
                            >
                                <td className="px-6 py-4">
                                    <div className="flex items-center space-x-3">
                                        {post.media && post.media.length > 0 ? (
                                            <img src={post.media[0]} alt="" className="w-12 h-12 rounded-lg object-cover" />
                                        ) : (
                                            <div className="w-12 h-12 rounded-lg bg-gray-200 flex items-center justify-center text-gray-400 text-xs">No Img</div>
                                        )}
                                        <p className="text-sm font-medium text-gray-900 truncate max-w-xs" title={post.caption}>{post.caption || 'Không có nội dung'}</p>
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center space-x-2">
                                        {post.author?.avatar ? (
                                            <img src={post.author.avatar} alt="" className="w-8 h-8 rounded-full object-cover" />
                                        ) : (
                                            <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 text-xs font-bold">
                                                {post.author?.name?.charAt(0) || '?'}
                                            </div>
                                        )}
                                        <span className="text-sm text-gray-700">{post.author?.name || 'Không rõ'}</span>
                                    </div>
                                </td>
                                <td className="px-6 py-4">
                                    <span className="px-2 py-1 text-xs font-medium rounded-full bg-blue-100 text-blue-700">
                                        {post.type}
                                    </span>
                                </td>
                                <td className="px-6 py-4">
                                    <div className="flex items-center space-x-4 text-sm text-gray-500">
                                        <span className="flex items-center"><Heart size={14} className="mr-1" /> {post.like}</span>
                                        <span className="flex items-center"><MessageSquare size={14} className="mr-1" /> {post.comment}</span>
                                    </div>
                                </td>
                                <td className="px-6 py-4 text-sm text-gray-600">{post.location?.name || 'N/A'}</td>
                                <td className="px-6 py-4 text-sm text-gray-500">
                                    {post.createdAt ? new Date(post.createdAt).toLocaleDateString('vi-VN') : 'N/A'}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default Posts;
