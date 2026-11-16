---
from: go
status: draft
concept: "Functions triggered by events, connected by managed services"
from_way: "A Go server with goroutines consuming from NATS or Kafka."
native_way: "Functions built with aws-lambda-go on the `provided.al2023` runtime, fed by API Gateway, SQS and EventBridge."
sensei_tip: "Build a static `bootstrap` binary for `provided.al2023` on arm64; Go cold starts are short, and `lambda.Start(handler)` reuses globals across invocations."
---

| Go world | AWS |
| --- | --- |
| `http.HandleFunc` | an HTTP API route |
| a NATS subscriber | an SQS event source mapping |
| `events.SQSEvent` | the batch Lambda passes you |
| `events.SQSEventResponse` | partial batch failures |
