import axios from 'axios';

// Use relative path to leverage Vite proxy
// If in production, this might need to change, but for dev this fixes CORS
// import.meta.env.VITE_API_URL || 
const api = axios.create({
    baseURL: 'http://34.124.175.170', // Use env var in prod, proxy in dev
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // Important for cookies
});

export default api;
