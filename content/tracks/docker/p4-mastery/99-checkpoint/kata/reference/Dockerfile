FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/catalog .

FROM alpine:3.24
RUN adduser -D -u 10001 app \
    && mkdir /data \
    && chown app:app /data
COPY --from=build /out/catalog /usr/local/bin/catalog
COPY --chmod=0755 docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
ENV PORT=8080 \
    DATA_DIR=/data \
    LOG_FORMAT=json \
    MIGRATE_ON_START=true
USER app
VOLUME /data
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=2s --start-period=10s --start-interval=1s --retries=3 \
    CMD ["catalog", "healthcheck"]
ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["serve"]
