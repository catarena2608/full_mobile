import React, { useState, useEffect } from 'react';
import { Flag, AlertTriangle, CheckCircle, XCircle, Search, Filter, AlertOctagon, Ban } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import Modal from '../components/Modal';

const Reports = () => {
    const navigate = useNavigate();
    const [reports, setReports] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [filter, setFilter] = useState('all'); // all, user, post, comment

    // Modal States (Only for Violation Actions now)
    const [selectedReport, setSelectedReport] = useState(null);
    const [isViolationModalOpen, setIsViolationModalOpen] = useState(false);

    // Violation Form States
    const [violationType, setViolationType] = useState('warn'); // warn, ban
    const [violationReason, setViolationReason] = useState('');
    const [violationExpiry, setViolationExpiry] = useState('');

    useEffect(() => {
        fetchReports();
    }, []);

    const fetchReports = async () => {
        try {
            setLoading(true);
            const response = await api.get('/stat/report');
            if (response.data.success) {
                setReports(response.data.reports || []);
            } else {
                setError('Failed to fetch reports');
            }
        } catch (err) {
            console.error("Error fetching reports:", err);
            setError('Error connecting to server');
        } finally {
            setLoading(false);
        }
    };

    const handleViewDetails = (report) => {
        navigate(`/report/${report._id}`);
    };

    const handleOpenViolationModal = (report, type) => {
        setSelectedReport(report);
        setViolationType(type);
        setViolationReason('');
        setViolationExpiry('');
        setIsViolationModalOpen(true);
    };

    const handleSubmitViolation = async () => {
        if (!violationReason) return alert('Please enter a reason');

        try {
            const payload = {
                reportID: selectedReport._id,
                targetID: selectedReport.target,
                type: selectedReport.type, // user, post, comment
                action: violationType, // warn, ban
                reason: violationReason,
                expiry: violationExpiry || null
            };

            const response = await api.post('/stat/report/violate', payload);

            if (response.data.success) {
                alert(`${violationType.toUpperCase()} action applied successfully`);
                setIsViolationModalOpen(false);
                fetchReports(); // Refresh list
            } else {
                alert('Failed to apply action: ' + response.data.message);
            }
        } catch (error) {
            console.error('Error submitting violation:', error);
            alert('Error submitting violation');
        }
    };

    const filteredReports = filter === 'all'
        ? reports
        : reports.filter(r => r.type === filter);

    if (loading) return <div className="p-8 text-center">Loading reports...</div>;

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-gray-800">Quản Lý Báo Cáo</h1>
                <div className="flex space-x-2">
                    <select
                        value={filter}
                        onChange={(e) => setFilter(e.target.value)}
                        className="border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-orange-500"
                    >
                        <option value="all">Tất Cả</option>
                        <option value="user">Người Dùng</option>
                        <option value="post">Bài Viết</option>
                        <option value="comment">Bình Luận</option>
                    </select>
                </div>
            </div>

            {error && (
                <div className="bg-red-50 text-red-600 p-4 rounded-lg border border-red-100">
                    {error}
                </div>
            )}

            <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-100">
                <div className="overflow-x-auto">
                    <table className="w-full text-left">
                        <thead>
                            <tr className="bg-gray-50 border-b border-gray-100">
                                <th className="p-4 font-semibold text-gray-600">Loại</th>
                                <th className="p-4 font-semibold text-gray-600">ID Mục Tiêu</th>
                                <th className="p-4 font-semibold text-gray-600">Lý Do</th>
                                <th className="p-4 font-semibold text-gray-600">Người Báo Cáo</th>
                                <th className="p-4 font-semibold text-gray-600">Ngày</th>
                                <th className="p-4 font-semibold text-gray-600">Hành Động</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {filteredReports.length > 0 ? (
                                filteredReports.map((report) => (
                                    <tr key={report._id} className="hover:bg-gray-50 transition-colors">
                                        <td className="p-4">
                                            <span className={`px-2 py-1 rounded-full text-xs font-medium capitalize
                                                ${report.type === 'user' ? 'bg-purple-100 text-purple-800' :
                                                    report.type === 'post' ? 'bg-blue-100 text-blue-800' : 'bg-orange-100 text-orange-800'}`}>
                                                {report.type === 'user' ? 'Người Dùng' : report.type === 'post' ? 'Bài Viết' : 'Bình Luận'}
                                            </span>
                                        </td>
                                        <td className="p-4 text-sm text-gray-600 font-mono">
                                            {report.target?.substring(0, 8)}...
                                        </td>
                                        <td className="p-4 text-gray-800">{report.reason}</td>
                                        <td className="p-4 text-sm text-gray-500">
                                            {report.reporterID ? report.reporterID.substring(0, 8) + '...' : 'Ẩn danh'}
                                        </td>
                                        <td className="p-4 text-sm text-gray-500">
                                            {new Date(report.createdAt).toLocaleDateString('vi-VN')}
                                        </td>
                                        <td className="p-4">
                                            <div className="flex space-x-2">
                                                <button
                                                    onClick={() => handleViewDetails(report)}
                                                    className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                                                >
                                                    Xem
                                                </button>
                                                <button
                                                    onClick={() => handleOpenViolationModal(report, 'warn')}
                                                    className="text-orange-600 hover:text-orange-800 text-sm font-medium flex items-center"
                                                >
                                                    <AlertTriangle size={14} className="mr-1" /> Cảnh Báo
                                                </button>
                                                <button
                                                    onClick={() => handleOpenViolationModal(report, 'ban')}
                                                    className="text-red-600 hover:text-red-800 text-sm font-medium flex items-center"
                                                >
                                                    <Ban size={14} className="mr-1" /> Cấm
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan="6" className="p-8 text-center text-gray-500">
                                        Không tìm thấy báo cáo nào.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Violation Action Modal */}
            <Modal
                isOpen={isViolationModalOpen}
                onClose={() => setIsViolationModalOpen(false)}
                title={`Xác Nhận ${violationType === 'warn' ? 'Cảnh Báo' : 'Cấm'}`}
            >
                <div className="space-y-4">
                    <div className={`p-4 rounded-lg flex items-center ${violationType === 'warn' ? 'bg-orange-50 text-orange-800' : 'bg-red-50 text-red-800'}`}>
                        {violationType === 'warn' ? <AlertTriangle className="mr-3" /> : <Ban className="mr-3" />}
                        <div>
                            <p className="font-bold">Bạn sắp {violationType === 'warn' ? 'cảnh báo' : 'cấm'} {selectedReport?.type === 'user' ? 'người dùng' : selectedReport?.type === 'post' ? 'bài viết' : 'bình luận'} này.</p>
                            <p className="text-sm">ID Mục Tiêu: {selectedReport?.target}</p>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Lý do {violationType === 'warn' ? 'cảnh báo' : 'cấm'}</label>
                        <textarea
                            className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                            rows="3"
                            placeholder="Nhập lý do..."
                            value={violationReason}
                            onChange={(e) => setViolationReason(e.target.value)}
                        ></textarea>
                    </div>

                    {violationType === 'ban' && (
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Hết Hạn Cấm (Tùy chọn)</label>
                            <input
                                type="date"
                                className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                                value={violationExpiry}
                                onChange={(e) => setViolationExpiry(e.target.value)}
                            />
                            <p className="text-xs text-gray-500 mt-1">Để trống nếu cấm vĩnh viễn.</p>
                        </div>
                    )}

                    <div className="flex justify-end space-x-3 pt-4">
                        <button
                            onClick={() => setIsViolationModalOpen(false)}
                            className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg"
                        >
                            Hủy
                        </button>
                        <button
                            onClick={handleSubmitViolation}
                            className={`px-4 py-2 text-white rounded-lg font-medium 
                                ${violationType === 'warn' ? 'bg-orange-500 hover:bg-orange-600' : 'bg-red-600 hover:bg-red-700'}`}
                        >
                            Xác Nhận {violationType === 'warn' ? 'Cảnh Báo' : 'Cấm'}
                        </button>
                    </div>
                </div>
            </Modal>
        </div>
    );
};

export default Reports;
