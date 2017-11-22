FROM nginx
RUN apt-get update && apt-get install -y curl
HEALTHCHECK --interval=2s --timeout=5s CMD curl -f http://localhost:80 || exit 1