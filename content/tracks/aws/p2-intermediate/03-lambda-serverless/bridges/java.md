---
from: java
status: draft
concept: "Functions triggered by events, connected by managed services"
from_way: "A Spring Boot app runs all day, with @Scheduled jobs and @JmsListener consumers inside it."
native_way: "Each handler is a Lambda function triggered by API Gateway, SQS, EventBridge or a schedule; Step Functions chain them."
sensei_tip: "Turn on SnapStart for Java functions and keep frameworks light (or use Spring Cloud Function with care); initialise clients in the constructor, not in `handleRequest`."
---

| Java world | AWS |
| --- | --- |
| `@JmsListener` | an SQS event source mapping |
| `@Scheduled(cron = ...)` | EventBridge Scheduler |
| `ApplicationEventPublisher` | EventBridge or SNS |
| a long-running batch job | Step Functions or AWS Batch |
