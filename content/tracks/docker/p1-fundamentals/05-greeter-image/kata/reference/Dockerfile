FROM alpine:3.24
ENV GREETING=hello
WORKDIR /app
COPY --chmod=755 . .
ENTRYPOINT ["/app/greet.sh"]
CMD ["world"]
