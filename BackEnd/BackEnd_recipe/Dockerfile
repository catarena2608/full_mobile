FROM node:18-slim

WORKDIR /app/BackEnd_recipe

COPY package*.json ./

RUN npm install

COPY . .
# Comment added to force cache bust
EXPOSE 5001

CMD ["node", "server.js"]