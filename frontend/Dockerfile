# syntax=docker/dockerfile:1
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .

# Vite inlines these at build time. They are public settings, never secrets.
ARG VITE_OIDC_ISSUER=
ARG VITE_OIDC_CLIENT_ID=dispute-portal-web
ARG VITE_DEV_LOGIN=false
ENV VITE_OIDC_ISSUER=$VITE_OIDC_ISSUER \
    VITE_OIDC_CLIENT_ID=$VITE_OIDC_CLIENT_ID \
    VITE_DEV_LOGIN=$VITE_DEV_LOGIN
RUN npm run build

# nginx-unprivileged runs as a non-root user and listens on 8080.
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
ENV OIDC_ORIGIN=http://localhost:8180
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=5 CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
