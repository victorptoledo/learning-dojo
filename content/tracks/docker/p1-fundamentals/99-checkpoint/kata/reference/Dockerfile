FROM golang:1.27-alpine
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN go build -o /usr/local/bin/server .
ENV PORT=8080 \
    GREETING_NAME=dojo
EXPOSE 8080
CMD ["server"]
