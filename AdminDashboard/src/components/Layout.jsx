import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';

const Layout = () => {
    return (
        <div className="min-h-screen bg-gray-100">
            <Sidebar />
            <div className="ml-64 p-8">
                <Outlet />
            </div>
        </div>
    );
};

export default Layout;
