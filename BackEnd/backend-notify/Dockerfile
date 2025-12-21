FROM node:22-slim

WORKDIR /app/BackEnd_notify

COPY package*.json ./

RUN npm install

COPY . .
# Comment added to force cache bust
EXPOSE 6001

CMD ["node", "server.js"]