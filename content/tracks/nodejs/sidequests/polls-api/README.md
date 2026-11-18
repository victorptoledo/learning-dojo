# Sidequest: Polls API

An API is mostly its edges: what it says when the input is wrong, what happens when a client
retries, how a list behaves while new data arrives, and what stops two people from
overwriting each other. Build a polls API on `node:http` alone, without a framework, and get
those edges right.

```text
$ curl -i -X POST localhost:3000/polls -H "content-type: application/json" \
       -d '{"question":"Tabs or spaces?","options":["Tabs","Spaces","Tabs"]}'
HTTP/1.1 422 Unprocessable Entity
content-type: application/problem+json

{"type":"about:blank","title":"Unprocessable Entity","status":422,
 "detail":"the body is not valid","errors":[{"path":"options[2]","message":"is a duplicate"}]}
```

Everything lives in `polls.ts`: `createApp(options)` returns a `node:http` `Server` that keeps
its polls in memory, and `main.ts` starts it (`npm start`). The contract tests start a fresh
app on a random port for each test, call it with `fetch` (and raw `node:http` requests for the
races), and move the app's clock through `options.now`. Read the clock only through `now()`.
The routes, status codes and JSON shapes are fixed; how you route, validate and store is
yours. No libraries. `tests/` is yours too.
