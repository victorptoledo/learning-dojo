# Works, but ships the toolchain and the source and runs as root.
# todo: turn this into a multi-stage build (see README.md)
FROM golang:1.27-alpine
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN go build -o /usr/local/bin/server .
ENV PORT=8080 \
    GREETING_NAME=dojo
EXPOSE 8080
CMD ["server"]
