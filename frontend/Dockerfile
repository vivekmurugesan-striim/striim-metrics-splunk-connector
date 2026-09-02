# Build stage
FROM node:18-alpine AS builder

WORKDIR /app

COPY frontend/package.json ./

# Try to copy package-lock.json if it exists, continue if not
COPY frontend/package-lock.json* ./

# Install dependencies (npm ci if lock file exists, npm install otherwise)
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY frontend/ .
RUN npm run build

# Runtime stage
FROM nginx:alpine

COPY --from=builder /app/build /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]
