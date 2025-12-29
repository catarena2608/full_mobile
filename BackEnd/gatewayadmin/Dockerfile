FROM node:18-slim

WORKDIR /app/gatewayAdmin

COPY package*.json ./

RUN npm install

COPY . .
# Comment added to force cache bust
EXPOSE 9000

CMD ["node", "server.js"]