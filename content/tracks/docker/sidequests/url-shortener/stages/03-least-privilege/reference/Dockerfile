FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/shortener .

FROM gcr.io/distroless/static-debian13:nonroot
COPY --from=build /out/shortener /shortener
USER 65532:65532
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/shortener"]
