import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Users from './pages/Users';
import Posts from './pages/Posts';
import Recipes from './pages/Recipes';
import TopSearch from './pages/TopSearch';



import Login from './pages/Login';
import Reports from './pages/Reports';
import PostDetail from './pages/PostDetail';
import UserDetail from './pages/UserDetail';
import ReportDetail from './pages/ReportDetail';
import RecipeDetail from './pages/RecipeDetail';
import CommentDetail from './pages/CommentDetail';

// Placeholder pages
const Placeholder = ({ title }) => (
  <div className="bg-white p-6 rounded-xl shadow-sm">
    <h2 className="text-2xl font-bold mb-4">{title}</h2>
    <p className="text-gray-500">Content coming soon...</p>
  </div>
);

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="users" element={<Users />} />
          <Route path="posts" element={<Posts />} />
          <Route path="recipes" element={<Recipes />} />
          <Route path="top-search" element={<TopSearch />} />
          <Route path="reports" element={<Reports />} />
          <Route path="post/:id" element={<PostDetail />} />
          <Route path="user/:id" element={<UserDetail />} />
          <Route path="report/:id" element={<ReportDetail />} />
          <Route path="recipe/:id" element={<RecipeDetail />} />
          <Route path="comment/:id" element={<CommentDetail />} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;
