FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY *.go ./
RUN --mount=type=cache,target=/root/.cache/go-build \
    CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/orders .

FROM gcr.io/distroless/static-debian13:nonroot
COPY --from=build /out/orders /app/orders
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/orders"]
CMD ["api"]
