FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/server .

FROM gcr.io/distroless/static-debian13:nonroot
COPY --from=build /out/server /app/server
ENV PORT=8080 \
    GREETING_NAME=dojo
EXPOSE 8080
ENTRYPOINT ["/app/server"]
