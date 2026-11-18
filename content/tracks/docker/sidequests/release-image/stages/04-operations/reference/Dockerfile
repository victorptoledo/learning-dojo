FROM golang:1.27-alpine AS build
ARG VERSION=2.3.0
WORKDIR /src
COPY go.mod ./
COPY cmd/ ./cmd/
COPY internal/ ./internal/
RUN --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 go build -trimpath -ldflags="-s -w -X main.version=${VERSION}" \
        -o /out/inventory ./cmd/inventory \
    && mkdir -p /out/data /out/tmp

FROM scratch
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=build /usr/local/go/lib/time/zoneinfo.zip /usr/share/zoneinfo.zip
COPY --from=build --chmod=1777 /out/tmp /tmp
COPY --from=build --chown=10001:10001 /out/data /var/lib/inventory
COPY --from=build /out/inventory /inventory
ENV PORT=8080 \
    DATA_DIR=/var/lib/inventory \
    ZONEINFO=/usr/share/zoneinfo.zip
USER 10001:10001
VOLUME /var/lib/inventory
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=2s --retries=3 CMD ["/inventory", "-healthcheck"]
ENTRYPOINT ["/inventory"]
