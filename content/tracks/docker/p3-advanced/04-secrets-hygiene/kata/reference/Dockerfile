FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN CGO_ENABLED=0 go build -trimpath -o /out/server .

FROM alpine:3.24
RUN adduser -D -u 10001 app
COPY --from=build /out/server /app/server
USER app
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/server"]
