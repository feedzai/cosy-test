FROM nginx
RUN apt-get update && apt-get install -y curl
HEALTHCHECK --interval=1s --timeout=5s CMD curl -f http://localhost:8080 || exit 1