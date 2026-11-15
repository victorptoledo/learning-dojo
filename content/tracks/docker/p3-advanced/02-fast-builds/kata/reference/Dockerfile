ARG VERSION=1.4.0

FROM golang:1.27-alpine AS build
ARG VERSION
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 go build -trimpath -ldflags="-s -w -X main.version=${VERSION}" -o /out/server .

FROM gcr.io/distroless/static-debian13:nonroot
ARG VERSION
LABEL org.opencontainers.image.version="${VERSION}"
COPY --from=build /out/server /app/server
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/server"]
