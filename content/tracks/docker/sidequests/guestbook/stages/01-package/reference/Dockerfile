FROM golang:1.27-alpine
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN go build -o /usr/local/bin/guestbook .
ENV PORT=8080 \
    BOARD_TITLE="Dojo board"
EXPOSE 8080
CMD ["guestbook"]
