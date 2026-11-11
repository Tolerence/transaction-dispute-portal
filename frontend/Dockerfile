# syntax=docker/dockerfile:1

# The bundle is built on the host with `make build` (npm). Vite inlines the VITE_* settings at that
# build step, so change them in .env and rebuild rather than passing them to Docker.

# nginx-unprivileged runs as a non-root user and listens on 8080.
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
ENV OIDC_ORIGIN=http://localhost:8180
COPY dist/ /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=5 CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
