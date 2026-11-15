# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 8 right.
2. **The integration kata** (press **Enter**): ship the Go service as a production-ready
   image (multi-stage, distroless, non-root, labelled, health-checked) and describe its
   stack with Redis in `compose.yaml`.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Dockerfile Best Practices | What invalidates the cache? Why copy the dependency manifests first? Why `apt-get update` and `install` together? |
| Multi-stage Builds | What reaches the final stage? When distroless, when alpine, when scratch? |
| Image Hardening | How do you run as non-root and still write to a volume? What does a health check need in a shell-less image? |
| Docker Compose | How do services find each other? What is the difference between `.env`, `environment` and secrets? What does `service_healthy` wait for? |

> [!TIP]
> In the kata, build and run the image yourself first (`docker build -t greeter .` then
> `docker run --rm -p 8080:8080 greeter`) and watch `docker ps` until the status says
> `healthy`. `docker compose config` shows how your `compose.yaml` is understood.
