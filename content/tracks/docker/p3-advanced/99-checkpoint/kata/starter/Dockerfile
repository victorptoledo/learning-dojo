# It builds and it runs, but the release review sent it back (see README.md).
FROM golang:1.27-alpine AS build
ARG GIT_TOKEN
ARG VERSION=3.1.0
ENV GOPRIVATE=git.dojo.example
WORKDIR /src
COPY . .
RUN printf 'machine git.dojo.example login ci password %s\n' "$GIT_TOKEN" > /root/.netrc \
    && CGO_ENABLED=0 go build -o /out/quotes .

FROM alpine:3.24
COPY --from=build /out/quotes /app/quotes
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/quotes"]
