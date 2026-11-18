FROM golang:1.27-alpine AS build
ARG VERSION=1.0.0
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w -X main.version=${VERSION}" -o /out/shortener .

FROM gcr.io/distroless/static-debian13:nonroot
ARG VERSION=1.0.0
LABEL org.opencontainers.image.title="shortener" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.source="https://github.com/dojo-example/shortener"
COPY --from=build /out/shortener /shortener
USER 65532:65532
ENV PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=2s --retries=3 CMD ["/shortener", "-healthcheck"]
ENTRYPOINT ["/shortener"]
