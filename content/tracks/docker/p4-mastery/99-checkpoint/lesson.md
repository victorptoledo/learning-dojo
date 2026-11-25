# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards
   production: PID 1 and signals, logs, migrations, rolling updates, health checks and the
   runtime underneath. You need 7 right.
2. **The integration kata** (press **Enter**): launch day for a catalog service. Make the
   image survive production (an entrypoint script that migrates and then *execs* the server,
   JSON logs on stdout, a writable volume for a non-root user, a health check with a start
   period) and write the Compose file of the rollout: a one-shot migration job, two
   replicas without a fixed port, start-first updates with rollback, a drain period and log
   rotation.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Logs and Debugging Containers | Where do logs go, and who rotates them? Which commands tell you why a container exited? |
| Docker in Production | Who is PID 1, and who receives `SIGTERM`? How does a rolling update decide a new task is ready? What sits under `dockerd`? |
| Incident: the Image that Will Not Stay Up (kata) | Which user runs each line, and who owns the paths it writes? |
| Runtime Security, Hardened Compose | Which settings keep the replicas safe as well as running? |
| Multi-stage Builds, Image Hardening | What does the final image really need? |

> [!TIP]
> `docker exec CONTAINER ps -o pid,user,args` shows at once who is PID 1. If it is
> `/bin/sh`, the server will never see the `SIGTERM` of `docker stop`.
