# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions on BuildKit, the supply chain, secrets and
   runtime security. You need 7 right.
2. **The integration kata** (press **Enter**): a release build of a service whose build
   needs private Git credentials. Pin the base images, keep the compiler cache, pass the
   credentials as a BuildKit secret that no layer records, stamp the version into the binary
   and the labels, and run it from Compose with secrets as files, a fixed user, a read-only
   file system, no capabilities and limits.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| BuildKit | What is the difference between a cache mount and a layer? Why does a secret mount leave nothing behind? In which stages is an `ARG` visible? |
| Fast Builds (kata) | Which `COPY` invalidates which step? |
| The Image Supply Chain | Why is a tag not a version? What do an SBOM, a scan and a signature each prove? |
| Secrets Hygiene (kata) | Where can a secret hide: files, `ENV`, `ARG`, history, the build context? |
| Runtime Security | What do `cap_drop`, `no-new-privileges`, `read_only` and the limits each stop? |
| Hardened Compose (kata) | Which findings would a security review raise on this file? |

> [!TIP]
> `docker history --no-trunc IMAGE` shows what each layer recorded, including the build
> arguments a `RUN` used. Run it on the starter's image and again on yours.
