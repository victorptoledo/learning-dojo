FROM golang:1.27-alpine
LABEL org.opencontainers.image.title="guestbook" \
      org.opencontainers.image.description="A small message board"
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN go build -o /usr/local/bin/guestbook .
ENV PORT=8080 \
    BOARD_TITLE="Dojo board"
EXPOSE 8080
ENTRYPOINT ["guestbook"]
