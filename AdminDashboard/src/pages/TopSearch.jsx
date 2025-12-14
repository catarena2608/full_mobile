import React, { useState, useEffect } from 'react';
import { Search, Filter } from 'lucide-react';
import api from '../services/api';

const TopSearch = () => {
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Filters
    const [type, setType] = useState('');
    const [day, setDay] = useState('');
    const [month, setMonth] = useState('');
    const [year, setYear] = useState('');

    useEffect(() => {
        fetchTopSearch();
    }, [type, day, month, year]);

    const fetchTopSearch = async () => {
        try {
            setLoading(true);
            setError(null);

            const params = {};
            if (type) params.type = type;
            if (day) params.day = day;
            if (month) params.month = month;
            if (year) params.year = year;

            const response = await api.get('/stat/search', { params });

            if (response.data.success) {
                setData(response.data.data || []);
            } else {
                setError('Failed to fetch data');
            }
        } catch (err) {
            console.error("Error fetching top search:", err);
            setError(err.message || 'Error fetching data');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="space-y-6">
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                <h2 className="text-2xl font-bold text-gray-800">Thống Kê Tìm Kiếm Hàng Đầu</h2>

                {/* Filters */}
                <div className="flex flex-wrap gap-2">
                    <select
                        className="p-2 border rounded-lg bg-white shadow-sm focus:ring-2 focus:ring-blue-500 outline-none"
                        value={type}
                        onChange={(e) => setType(e.target.value)}
                    >
                        <option value="">Tất Cả</option>
                        <option value="name">Tên</option>
                        <option value="user_name">Tên người dùng</option>
                        <option value="tag">Thẻ</option>
                    </select>

                    <input
                        type="number"
                        placeholder="Ngày"
                        className="p-2 w-20 border rounded-lg bg-white shadow-sm focus:ring-2 focus:ring-blue-500 outline-none"
                        value={day}
                        onChange={(e) => setDay(e.target.value)}
                    />
                    <input
                        type="number"
                        placeholder="Tháng"
                        className="p-2 w-20 border rounded-lg bg-white shadow-sm focus:ring-2 focus:ring-blue-500 outline-none"
                        value={month}
                        onChange={(e) => setMonth(e.target.value)}
                    />
                    <input
                        type="number"
                        placeholder="Năm"
                        className="p-2 w-24 border rounded-lg bg-white shadow-sm focus:ring-2 focus:ring-blue-500 outline-none"
                        value={year}
                        onChange={(e) => setYear(e.target.value)}
                    />
                </div>
            </div>

            {/* Content */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                {loading ? (
                    <div className="p-10 text-center text-gray-500">Đang tải thống kê...</div>
                ) : error ? (
                    <div className="p-10 text-center text-red-500">
                        Lỗi: {error} <br />
                        <span className="text-sm text-gray-400">Đảm bảo BackEnd_static đang chạy trên cổng 7001</span>
                    </div>
                ) : data.length === 0 ? (
                    <div className="p-10 text-center text-gray-500">Không tìm thấy dữ liệu cho các bộ lọc này.</div>
                ) : (
                    <table className="w-full text-left">
                        <thead className="bg-gray-50 border-b border-gray-100">
                            <tr>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Từ Khóa</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Loại</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Số Lượng</th>
                                <th className="px-6 py-4 text-xs font-semibold text-gray-500 uppercase tracking-wider">Tìm Kiếm Lần Cuối</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {data.map((item, index) => (
                                <tr key={item._id || index} className="hover:bg-gray-50 transition-colors">
                                    <td className="px-6 py-4 font-medium text-gray-900">{item.target}</td>
                                    <td className="px-6 py-4">
                                        <span className={`px-2 py-1 rounded-full text-xs font-medium 
                                            ${item.type === 'tag' ? 'bg-purple-100 text-purple-700' :
                                                item.type === 'user_name' ? 'bg-blue-100 text-blue-700' :
                                                    'bg-green-100 text-green-700'}`}>
                                            {item.type}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 font-semibold text-blue-600">{item.count}</td>
                                    <td className="px-6 py-4 text-sm text-gray-500">
                                        {item.updatedAt ? new Date(item.updatedAt).toLocaleString('vi-VN') : 'N/A'}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
};

export default TopSearch;
