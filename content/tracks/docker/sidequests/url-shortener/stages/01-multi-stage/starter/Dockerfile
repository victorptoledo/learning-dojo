# Works, but ships the Go toolchain, the sources and the build cache: about 900 MB.
# todo: turn it into a multi-stage build (see README.md)
FROM golang:1.27
WORKDIR /src
COPY . .
RUN go build -o /usr/local/bin/shortener .
ENV PORT=8080
EXPOSE 8080
CMD ["shortener"]
