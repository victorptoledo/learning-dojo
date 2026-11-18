# The image the team ships today. It builds and runs, but it fails the release checklist.
FROM golang:1.27-alpine AS build
WORKDIR /src
COPY . .
RUN go build -o /out/inventory ./cmd/inventory

FROM alpine:3.24
COPY --from=build /out/inventory /usr/local/bin/inventory
ENV PORT=8080 \
    DATA_DIR=/var/lib/inventory
EXPOSE 8080
CMD inventory
