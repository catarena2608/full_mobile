import React, { useState, useEffect } from 'react';
import { Heart, MessageSquare } from 'lucide-react';
import api from '../services/api';

const PostDetailContent = ({ post }) => {
    const [comments, setComments] = useState([]);
    const [loadingComments, setLoadingComments] = useState(false);
    const [commentUsers, setCommentUsers] = useState({});

    useEffect(() => {
        fetchComments();
    }, [post._id]);

    const fetchComments = async () => {
        try {
            setLoadingComments(true);
            // 1. Fetch comments
            const res = await api.get(`/api/comment/${post._id}`);
            if (res.data.success) {
                const fetchedComments = res.data.comments || [];
                setComments(fetchedComments);

                // 2. Extract User IDs
                const userIds = [...new Set(fetchedComments.map(c => c.userID))];

                // 3. Fetch User Details if there are comments
                if (userIds.length > 0) {
                    const userRes = await api.get(`/api/userAdmin`, {
                        params: { ids: userIds.join(','), limit: userIds.length }
                    });
                    if (userRes.data.success) {
                        const userMap = {};
                        userRes.data.users.forEach(u => {
                            userMap[u._id] = u;
                        });
                        setCommentUsers(userMap);
                    }
                }
            }
        } catch (error) {
            console.error("Error fetching comments:", error);
        } finally {
            setLoadingComments(false);
        }
    };

    return (
        <div className="space-y-6 p-6">
            {post.media && post.media.length > 0 && (
                <div className="grid grid-cols-2 gap-2">
                    {post.media.map((url, i) => (
                        <img key={i} src={url} alt={`Media ${i}`} className="w-full h-48 object-cover rounded-lg" />
                    ))}
                </div>
            )}

            <div>
                <h4 className="font-semibold text-gray-800 mb-2">Nội Dung</h4>
                <p className="text-gray-600 bg-gray-50 p-4 rounded-lg">{post.caption}</p>
            </div>

            <div className="flex justify-between items-center text-sm text-gray-500 border-t pt-4">
                <div className="flex space-x-4">
                    <span className="flex items-center"><Heart size={16} className="mr-1 text-red-500" /> {post.like} Lượt thích</span>
                    <span className="flex items-center"><MessageSquare size={16} className="mr-1 text-blue-500" /> {post.comment} Bình luận</span>
                </div>
                <span>{new Date(post.createdAt).toLocaleString('vi-VN')}</span>
            </div>

            {/* Comments Section */}
            <div className="border-t pt-4">
                <h4 className="font-semibold text-gray-800 mb-4">Bình Luận</h4>
                {loadingComments ? (
                    <div className="text-center text-gray-500">Đang tải bình luận...</div>
                ) : comments.length > 0 ? (
                    <div className="space-y-4 max-h-60 overflow-y-auto pr-2">
                        {comments.map(comment => {
                            const user = commentUsers[comment.userID];
                            return (
                                <div key={comment._id} className="flex space-x-3 bg-gray-50 p-3 rounded-lg">
                                    <div className="flex-shrink-0">
                                        {user?.avatar ? (
                                            <img src={user.avatar} alt={user.name} className="w-8 h-8 rounded-full object-cover" />
                                        ) : (
                                            <div className="w-8 h-8 rounded-full bg-gray-300 flex items-center justify-center text-xs font-bold text-gray-600">
                                                {user?.name?.charAt(0) || '?'}
                                            </div>
                                        )}
                                    </div>
                                    <div>
                                        <div className="flex items-center space-x-2">
                                            <span className="text-sm font-semibold text-gray-900">{user?.name || 'Người dùng ẩn danh'}</span>
                                            <span className="text-xs text-gray-500">{new Date(comment.createdAt).toLocaleDateString('vi-VN')}</span>
                                        </div>
                                        <p className="text-sm text-gray-700 mt-1">{comment.content}</p>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                ) : (
                    <div className="text-center text-gray-500 italic">Chưa có bình luận nào.</div>
                )}
            </div>
        </div>
    );
};

export default PostDetailContent;
