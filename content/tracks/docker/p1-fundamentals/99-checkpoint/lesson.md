# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 8 right.
2. **The integration kata** (press **Enter**): containerise a small Go web service with a
   Dockerfile that builds it, configures it and starts it the right way.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| What a Container Is | What do namespaces hide, and what do cgroups limit? What is shared with the host? |
| Images and Layers | Why does deleting a file in a later step not shrink an image? Tag or digest? |
| The Container Lifecycle | What does `docker stop` send, and why does PID 1 matter? What do exit codes 137 and 143 mean? |
| Dockerfile Basics | What is the build context? How do `ENTRYPOINT` and `CMD` combine? Shell or exec form? |
| Volumes | Which data survives `docker rm`? When a volume, when a bind mount? |
| Networking | What is `localhost` in a container? How do containers find each other by name? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
