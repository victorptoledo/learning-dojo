ARG VERSION=3.1.0
ARG REVISION=unknown

FROM golang:1.27-alpine@sha256:70bd7ddea8b6cbe5d6943cde279d6b67feb10cc924fb9100d1d8757c1575b280 AS build
ARG VERSION
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN --mount=type=cache,target=/go/pkg/mod \
    --mount=type=cache,target=/root/.cache/go-build \
    --mount=type=secret,id=netrc,target=/root/.netrc \
    GOPRIVATE=git.dojo.example CGO_ENABLED=0 go build -trimpath -ldflags="-s -w -X main.version=${VERSION}" -o /out/quotes .

FROM alpine:3.24@sha256:f59abe656b692d901e184be3445e084786602108599d2a51d229c0f5a48c71bf
ARG VERSION
ARG REVISION
LABEL org.opencontainers.image.title="quotes" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${REVISION}" \
      org.opencontainers.image.source="https://git.dojo.example/platform/quotes"
COPY --from=build /out/quotes /app/quotes
ENV PORT=8080
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=2s --retries=3 CMD ["/app/quotes", "-healthcheck"]
ENTRYPOINT ["/app/quotes"]
