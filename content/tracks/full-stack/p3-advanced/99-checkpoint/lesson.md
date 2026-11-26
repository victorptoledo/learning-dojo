# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a live order tracker in Node.js, ready to run as
   two instances behind a load balancer: a list page that costs two queries, status changes
   pushed over Server-Sent Events that resume after a reconnect and fan out through a shared
   pub/sub, a gauge of open streams, and a drain for rolling deploys.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Deploying the Stack | What do liveness and readiness each tell the platform? What happens to open connections when an old instance stops? |
| Compose the Whole Stack (kata) | Which services must be healthy before the API serves? |
| Observability End to End | Is this number a counter, a gauge or a histogram? Which labels keep it cheap? |
| Traced Requests (kata) | What ties a log line, a metric and a trace to the same request? |
| Performance and Real-Time | Where does an N+1 hide? When do SSE beat WebSockets, and how do updates reach clients of another instance? |

> [!TIP]
> Draw one status change as a sequence: the POST lands on instance A, the client listens on
> instance B. Every arrow that is not "through the bus" is either a missed update or a
> duplicate.
