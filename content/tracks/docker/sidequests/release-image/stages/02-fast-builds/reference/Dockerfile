FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY cmd/ ./cmd/
COPY internal/ ./internal/
RUN --mount=type=cache,target=/root/.cache/go-build \
    go build -o /out/inventory ./cmd/inventory

FROM alpine:3.24
RUN addgroup -S -g 10001 inventory \
    && adduser -S -D -H -u 10001 -G inventory inventory \
    && mkdir -p /var/lib/inventory \
    && chown 10001:10001 /var/lib/inventory
COPY --from=build /out/inventory /usr/local/bin/inventory
ENV PORT=8080 \
    DATA_DIR=/var/lib/inventory
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["inventory"]
