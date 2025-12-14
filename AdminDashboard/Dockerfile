# --- Stage 1: Build React App ---
FROM node:18-alpine as builder

WORKDIR /app

# Copy package.json trước để tận dụng Docker cache
COPY package*.json ./
RUN npm install

# Copy toàn bộ source code và build
COPY . .
RUN npm run build

# --- Stage 2: Serve with Nginx ---
FROM nginx:alpine

# Copy file build từ Stage 1 sang thư mục html của Nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy file cấu hình Nginx custom (để xử lý React Router và API Proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80 (Port mặc định của web)
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]