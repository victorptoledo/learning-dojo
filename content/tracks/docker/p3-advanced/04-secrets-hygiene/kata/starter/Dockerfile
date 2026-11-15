# Works, but the API token ends up inside the image in three ways.
# todo: keep every secret out of the image (see README.md)
FROM golang:1.27-alpine AS build
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -o /out/server .

FROM alpine:3.24
ARG API_TOKEN=dojo-fake-token-7f3a91
ENV API_TOKEN=${API_TOKEN}
RUN adduser -D -u 10001 app
COPY --from=build /src /src
COPY secrets/api_token.txt /run/secrets/api_token
COPY --from=build /out/server /app/server
USER app
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/server"]
