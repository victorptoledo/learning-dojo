# Launch day: this image has to survive production (see README.md).
FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN CGO_ENABLED=0 go build -trimpath -o /out/catalog .

FROM alpine:3.24
RUN adduser -D -u 10001 app
COPY --from=build /out/catalog /usr/local/bin/catalog
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
ENV PORT=8080 \
    DATA_DIR=/data \
    LOG_FORMAT=text
USER app
VOLUME /data
EXPOSE 8080
HEALTHCHECK --interval=30s CMD wget -q -O /dev/null http://localhost:8080/ || exit 1
ENTRYPOINT docker-entrypoint.sh
