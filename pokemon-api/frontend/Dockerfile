FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL \
    VITE_USE_MOCKS=false
RUN npm run build && rm -f dist/mockServiceWorker.js

FROM nginx:1.27-alpine

ENV BACKEND_URL=http://backend:8080
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
