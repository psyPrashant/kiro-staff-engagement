# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency manifests first for layer caching
COPY package.json package-lock.json ./

# Install dependencies deterministically
RUN npm ci

# Copy remaining source
COPY . .

# Build the Angular production bundle
RUN npx ng build --configuration production

# Stage 2: Runtime
FROM nginxinc/nginx-unprivileged:alpine

# Copy compiled assets from builder stage
COPY --from=builder /app/dist/staff-engagement/browser/ /usr/share/nginx/html/

# Copy custom nginx configuration
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# Expose port 8080 (nginx-unprivileged listens on 8080)
EXPOSE 8080
