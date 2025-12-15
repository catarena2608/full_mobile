import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    proxy: {
      '/api': {
        target: 'http://35.197.129.194',
        changeOrigin: true,
      },
      '/stat': {
        target: 'http://34.124.175.170',
        changeOrigin: true,
      },
    }
  }
})
