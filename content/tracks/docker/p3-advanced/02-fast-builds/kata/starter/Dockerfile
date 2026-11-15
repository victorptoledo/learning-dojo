# Works, but rebuilds everything on any change and never uses VERSION.
# todo: make it cache-friendly and stamp the version (see README.md)
ARG VERSION=1.4.0

FROM golang:1.27-alpine AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -o /out/server .

FROM gcr.io/distroless/static-debian13:nonroot
COPY --from=build /out/server /app/server
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/server"]
