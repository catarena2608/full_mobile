import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, AlertTriangle, Ban, AlertOctagon } from 'lucide-react';
import api from '../services/api';
import Modal from '../components/Modal';

const ReportDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Violation Modal State
    const [isViolationModalOpen, setIsViolationModalOpen] = useState(false);
    const [violationType, setViolationType] = useState('warn');
    const [violationReason, setViolationReason] = useState('');
    const [violationExpiry, setViolationExpiry] = useState('');

    useEffect(() => {
        const fetchReport = async () => {
            try {
                setLoading(true);
                // Since we don't have a single report API, we fetch all and filter
                // Ideally this should be replaced by a specific API endpoint
                const response = await api.get('/stat/report');
                if (response.data.success) {
                    const foundReport = response.data.reports.find(r => r._id === id);
                    if (foundReport) {
                        setReport(foundReport);
                    } else {
                        setError('Report not found');
                    }
                } else {
                    setError('Failed to fetch reports');
                }
            } catch (err) {
                console.error("Error fetching report:", err);
                setError('Error connecting to server');
            } finally {
                setLoading(false);
            }
        };

        if (id) {
            fetchReport();
        }
    }, [id]);

    const handleViewTarget = () => {
        if (!report) return;
        if (report.type === 'post') {
            navigate(`/post/${report.target}`);
        } else if (report.type === 'user') {
            navigate(`/user/${report.target}`);
        } else if (report.type === 'comment') {
            navigate(`/comment/${report.target}`);
        }
    };

    const handleOpenViolationModal = (type) => {
        setViolationType(type);
        setViolationReason('');
        setViolationExpiry('');
        setIsViolationModalOpen(true);
    };

    const handleSubmitViolation = async () => {
        if (!violationReason) return alert('Please enter a reason');

        try {
            const payload = {
                reportID: report._id,
                targetID: report.target,
                type: report.type,
                action: violationType,
                reason: violationReason,
                expiry: violationExpiry || null
            };

            const response = await api.post('/stat/report/violate', payload);

            if (response.data.success) {
                alert(`${violationType.toUpperCase()} action applied successfully`);
                setIsViolationModalOpen(false);
                // Refresh report data?
                // navigate(-1); // Or stay?
            } else {
                alert('Failed to apply action: ' + response.data.message);
            }
        } catch (error) {
            console.error('Error submitting violation:', error);
            alert('Error submitting violation');
        }
    };

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
                <div className="flex items-start justify-between">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900 mb-2">Chi Tiết Báo Cáo</h1>
                        <p className="text-gray-500">ID: {report._id}</p>
                    </div>
                    <div className={`px-3 py-1 rounded-full text-sm font-medium capitalize
                        ${report.type === 'user' ? 'bg-purple-100 text-purple-800' :
                            report.type === 'post' ? 'bg-blue-100 text-blue-800' : 'bg-orange-100 text-orange-800'}`}>
                        {report.type === 'user' ? 'Người Dùng' : report.type === 'post' ? 'Bài Viết' : 'Bình Luận'}
                    </div>
                </div>

                <div className="mt-8 grid grid-cols-1 md:grid-cols-2 gap-8">
                    <div className="space-y-6">
                        <div>
                            <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2">Thông Tin Báo Cáo</h3>
                            <div className="bg-gray-50 p-4 rounded-xl space-y-3">
                                <div>
                                    <span className="text-gray-500 text-sm block">Người Báo Cáo</span>
                                    <span className="font-mono text-gray-900">{report.reporterID || 'Ẩn danh'}</span>
                                </div>
                                <div>
                                    <span className="text-gray-500 text-sm block">Ngày Tạo</span>
                                    <span className="text-gray-900">{new Date(report.createdAt).toLocaleString('vi-VN')}</span>
                                </div>
                            </div>
                        </div>

                        <div>
                            <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2">Lý Do</h3>
                            <div className="bg-red-50 p-4 rounded-xl text-red-900 border border-red-100">
                                {report.reason}
                            </div>
                        </div>
                    </div>

                    <div className="space-y-6">
                        <div>
                            <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2">Mục Tiêu Bị Báo Cáo</h3>
                            <div className="bg-white border border-gray-200 p-4 rounded-xl shadow-sm">
                                <p className="text-sm text-gray-500 mb-1">Target ID</p>
                                <p className="font-mono text-lg font-medium text-gray-900 mb-4">{report.target}</p>
                                <button
                                    onClick={handleViewTarget}
                                    className="w-full py-2 bg-blue-50 text-blue-600 rounded-lg font-medium hover:bg-blue-100 transition-colors"
                                >
                                    Xem Chi Tiết {report.type === 'user' ? 'Người Dùng' : report.type === 'post' ? 'Bài Viết' : 'Bình Luận'}
                                </button>
                            </div>
                        </div>

                        <div>
                            <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-2">Hành Động</h3>
                            <div className="grid grid-cols-2 gap-4">
                                <button
                                    onClick={() => handleOpenViolationModal('warn')}
                                    className="flex items-center justify-center py-3 bg-orange-50 text-orange-600 rounded-xl font-medium hover:bg-orange-100 transition-colors"
                                >
                                    <AlertTriangle size={18} className="mr-2" />
                                    Cảnh Báo
                                </button>
                                <button
                                    onClick={() => handleOpenViolationModal('ban')}
                                    className="flex items-center justify-center py-3 bg-red-50 text-red-600 rounded-xl font-medium hover:bg-red-100 transition-colors"
                                >
                                    <Ban size={18} className="mr-2" />
                                    Cấm
                                </button>
                            </div>
                        </div>
                    </div>
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
                            <p className="font-bold">Bạn sắp {violationType === 'warn' ? 'cảnh báo' : 'cấm'} {report?.type === 'user' ? 'người dùng' : report?.type === 'post' ? 'bài viết' : 'bình luận'} này.</p>
                            <p className="text-sm">ID Mục Tiêu: {report?.target}</p>
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

export default ReportDetail;
