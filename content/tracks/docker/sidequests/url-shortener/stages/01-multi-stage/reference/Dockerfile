FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN go build -o /out/shortener .

FROM alpine:3.24
COPY --from=build /out/shortener /usr/local/bin/shortener
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["shortener"]
