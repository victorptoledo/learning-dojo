---
from: python
status: draft
concept: "Shipping Java today"
from_way: "logging, environment settings, gunicorn workers and a python image."
native_way: "SLF4J with Logback, configuration from the environment, a runnable JAR or image, and health and metrics endpoints."
sensei_tip: "One JVM process with many threads replaces a pool of gunicorn worker processes. Size it with MaxRAMPercentage for the container's memory."
---

| Python | Java 25 |
| --- | --- |
| logging.getLogger(__name__) | LoggerFactory.getLogger(App.class) |
| os.environ['DB_URL'] | System.getenv("DB_URL") or framework config |
| gunicorn -w 4 | one JVM with a thread pool or virtual threads |
| python:3.13-slim | eclipse-temurin:25-jre or distroless java |
