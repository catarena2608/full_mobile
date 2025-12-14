import React, { useState, useEffect } from 'react';
import { Users, FileText, Heart, Activity, MoreHorizontal } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, Cell } from 'recharts';
import StatsCard from '../components/StatsCard';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';

const Dashboard = () => {
    const navigate = useNavigate();
    const [stats, setStats] = useState({
        users: 0,
        posts: 0,
        likes: 0,
        follows: 0
    });
    const [topSearchData, setTopSearchData] = useState([]);
    const [userGrowthData, setUserGrowthData] = useState([]);
    const [recentPosts, setRecentPosts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                // Fetch Stats
                const currentYear = new Date().getFullYear();
                const [userRes, postRes, likeRes, followRes, searchRes] = await Promise.all([
                    api.get('/api/userAdmin/countUser'),
                    api.get('/api/postAdmin/countPost'),
                    api.get('/api/postAdmin/countLike'),
                    api.get('/api/userAdmin/countFollow'),
                    api.get(`/stat/search?limit=10&year=${currentYear}`)
                ]);

                const userTotal = userRes.data.success ? userRes.data.total : 0;
                const postTotal = postRes.data.success ? postRes.data.total : 0;
                const likeTotal = likeRes.data.success ? likeRes.data.total : 0;
                const followTotal = followRes.data.success ? followRes.data.total : 0;

                setStats({
                    users: userTotal,
                    posts: postTotal,
                    likes: likeTotal,
                    follows: followTotal
                });

                // Process User Growth Data
                if (userRes.data.success && userRes.data.detail) {
                    const growthData = userRes.data.detail.map(item => ({
                        name: `Tháng ${item.month}`,
                        value: item.count
                    }));
                    setUserGrowthData(growthData);
                }

                // Fetch Recent Posts for Table
                try {
                    const postsListRes = await api.get('/api/postAdmin?limit=5');
                    if (postsListRes.data.success) {
                        const rawPosts = postsListRes.data.posts || [];

                        // Fetch authors for these posts
                        const userIds = [...new Set(rawPosts.map(p => p.userID))].filter(id => id);
                        let userMap = {};

                        if (userIds.length > 0) {
                            const userRes = await api.get('/api/userAdmin', {
                                params: { ids: userIds.join(','), limit: userIds.length }
                            });
                            if (userRes.data.success) {
                                userRes.data.users.forEach(u => {
                                    userMap[u._id] = u;
                                });
                            }
                        }

                        // Map posts with author info
                        const mappedPosts = rawPosts.map(post => ({
                            ...post,
                            author: userMap[post.userID] || { name: 'Unknown', avatar: null }
                        }));

                        setRecentPosts(mappedPosts);
                    }
                } catch (err) {
                    console.warn("Could not fetch recent posts list:", err);
                }

                // Process Top Search Data for Bar Chart
                if (searchRes.data.success) {
                    const rawData = searchRes.data.data || [];
                    // Transform for chart
                    const chartData = rawData.map(item => ({
                        name: item.target,
                        value: item.count,
                        color: '#3b82f6' // Default blue
                    })).slice(0, 10);

                    // Add colors gradient simulation
                    const colors = ['#2563eb', '#3b82f6', '#60a5fa', '#93c5fd', '#bfdbfe'];
                    chartData.forEach((item, index) => {
                        item.color = colors[index % colors.length];
                    });

                    setTopSearchData(chartData);
                }

            } catch (error) {
                console.error("Error fetching dashboard data:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    const handleRowClick = (post) => {
        navigate(`/post/${post._id}`);
    };

    if (loading) return <div className="flex justify-center items-center h-screen text-amber-500">Loading Dashboard...</div>;

    return (
        <div className="space-y-8">
            {/* Header Section */}
            <div>
                <h2 className="text-3xl font-bold text-gray-900">Tổng Quan Facecook</h2>
                <p className="text-gray-500 mt-1">Chào mừng trở lại! Đây là tổng quan về hoạt động trong năm {new Date().getFullYear()}.</p>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatsCard
                    title="Tổng Bài Viết"
                    value={stats.posts.toLocaleString()}
                    icon={FileText}
                    trend={12.5}
                />
                <StatsCard
                    title="Tổng Người Dùng"
                    value={stats.users.toLocaleString()}
                    icon={Users}
                    trend={15.2}
                />
                <StatsCard
                    title="Tổng Lượt Thích"
                    value={stats.likes.toLocaleString()}
                    icon={Heart}
                    trend={20.1}
                />
                <StatsCard
                    title="Tổng Lượt Theo Dõi"
                    value={stats.follows.toLocaleString()}
                    icon={Activity}
                    trend={5.3}
                />
            </div>

            {/* Charts Section */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Top Search Chart */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                    <h3 className="text-lg font-bold text-gray-900 mb-1">Top từ khóa tìm kiếm</h3>
                    <p className="text-sm text-gray-500 mb-6">Những từ khóa tìm kiếm nhiều trong năm {new Date().getFullYear()}</p>
                    <div className="h-80">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart layout="vertical" data={topSearchData} margin={{ top: 5, right: 30, left: 40, bottom: 5 }}>
                                <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f0f0f0" />
                                <XAxis type="number" hide />
                                <YAxis type="category" dataKey="name" width={100} tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                                <Tooltip cursor={{ fill: '#f9fafb' }} contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                <Bar dataKey="value" radius={[0, 4, 4, 0]} barSize={20}>
                                    {topSearchData.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={entry.color} />
                                    ))}
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* User Growth Chart */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                    <h3 className="text-lg font-bold text-gray-900 mb-1">Tăng trưởng người dùng</h3>
                    <p className="text-sm text-gray-500 mb-6">Số lượng người dùng mới theo tháng</p>
                    <div className="h-80">
                        <ResponsiveContainer width="100%" height="100%">
                            <LineChart data={userGrowthData}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                                <XAxis dataKey="name" stroke="#9ca3af" fontSize={12} tickLine={false} axisLine={false} />
                                <YAxis stroke="#9ca3af" fontSize={12} tickLine={false} axisLine={false} />
                                <Tooltip contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }} />
                                <Line type="monotone" dataKey="value" name="Người dùng mới" stroke="#10b981" strokeWidth={3} dot={{ r: 4, fill: '#10b981', strokeWidth: 2, stroke: '#fff' }} activeDot={{ r: 6 }} />
                            </LineChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </div>

            {/* Recent Posts Table */}
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                <div className="p-6 border-b border-gray-100 flex justify-between items-center">
                    <div>
                        <h3 className="text-lg font-bold text-gray-900">Bài Đăng Gần Đây</h3>
                        <p className="text-sm text-gray-500">Các bài đăng món ăn mới nhất</p>
                    </div>
                    <button className="p-2 hover:bg-gray-50 rounded-lg text-gray-400">
                        <MoreHorizontal size={20} />
                    </button>
                </div>
                <div className="overflow-x-auto">
                    <table className="w-full text-left">
                        <thead className="bg-gray-50/50">
                            <tr>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Mã Bài</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Người Đăng</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Nội dung</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Lượt Thích</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Trạng Thái</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Ngày</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {recentPosts.map((post) => (
                                <tr
                                    key={post._id}
                                    onClick={() => handleRowClick(post)}
                                    className="hover:bg-gray-50 transition-colors cursor-pointer"
                                >
                                    <td className="px-6 py-4 text-sm font-medium text-gray-900">
                                        <div className="flex items-center space-x-3">
                                            {post.media && post.media.length > 0 ? (
                                                <img src={post.media[0]} alt="" className="w-10 h-10 rounded-lg object-cover bg-gray-100" />
                                            ) : (
                                                <div className="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center text-gray-400 text-xs">No Img</div>
                                            )}
                                            <span className="text-gray-500">#{post._id.slice(-5)}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="flex items-center space-x-2">
                                            {post.author?.avatar ? (
                                                <img src={post.author.avatar} alt="" className="w-6 h-6 rounded-full object-cover" />
                                            ) : (
                                                <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 text-xs font-bold">
                                                    {post.author?.name?.charAt(0) || '?'}
                                                </div>
                                            )}
                                            <span className="text-sm text-gray-700">{post.author?.name || 'Không rõ'}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 text-sm text-gray-700 font-medium truncate max-w-xs" title={post.caption}>
                                        {post.caption || 'Không có nội dung'}
                                    </td>
                                    <td className="px-6 py-4 text-sm text-gray-600">
                                        <div className="flex space-x-3">
                                            <span className="flex items-center"><Heart size={14} className="mr-1" /> {post.like || 0}</span>
                                            <span className="flex items-center"><FileText size={14} className="mr-1" /> {post.comment || 0}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <span className={`px-3 py-1 rounded-full text-xs font-medium ${post.deleted ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                                            {post.deleted ? 'Đã xóa' : 'Hoạt động'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-sm text-gray-500">
                                        {post.createdAt ? new Date(post.createdAt).toLocaleDateString('vi-VN') : 'N/A'}
                                    </td>
                                </tr>
                            ))}
                            {recentPosts.length === 0 && (
                                <tr>
                                    <td colSpan="6" className="px-6 py-8 text-center text-gray-500">
                                        Chưa có bài đăng nào.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
