# The orders service image (already done: the kata is about the workflow).
FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/orders .

FROM gcr.io/distroless/static-debian13:nonroot
COPY --from=build /out/orders /orders
USER nonroot:nonroot
EXPOSE 8080
ENTRYPOINT ["/orders"]
