FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN CGO_ENABLED=0 go build -trimpath -o /out/server .

FROM alpine:3.24
RUN adduser -D -u 10001 app \
    && mkdir /data \
    && chown app:app /data
COPY --from=build /out/server /usr/local/bin/server
ENV PORT=8080 \
    DATA_DIR=/data
USER app
VOLUME /data
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=2s --start-period=5s --start-interval=1s --retries=3 \
    CMD ["server", "-healthcheck"]
CMD ["server"]
