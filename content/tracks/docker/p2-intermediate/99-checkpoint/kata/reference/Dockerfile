FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/server .

FROM gcr.io/distroless/static-debian13:nonroot
LABEL org.opencontainers.image.title="greeter" \
      org.opencontainers.image.version="1.0.0"
COPY --from=build /out/server /app/server
ENV PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=2s --start-period=5s --start-interval=1s --retries=3 \
    CMD ["/app/server", "-healthcheck"]
ENTRYPOINT ["/app/server"]
