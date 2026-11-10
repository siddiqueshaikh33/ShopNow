# Multi-stage Dockerfile for ShopNow React Frontend

# Stage 1: Build stage
FROM node:20-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY ShopNow_Frontend/SN_Frontend/package*.json ./
RUN npm ci

# Copy source and build
COPY ShopNow_Frontend/SN_Frontend/ .
RUN npm run build

# Stage 2: Production serve stage
FROM nginx:alpine
COPY ShopNow_Frontend/SN_Frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

