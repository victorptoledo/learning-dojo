# Sidequest: Event sign-ups

A community runs meetups with a fixed number of seats. Sign-ups open at a set time and the
popular ones fill in seconds, from phones on bad connections that retry. The API runs on
AWS Lambda behind an API Gateway HTTP API, with one DynamoDB table. Two promises matter:
**never more sign-ups than seats**, and **a retried request never signs anyone up twice**.
You build it in four stages:

1. `handler` creates events, shows them and signs people up, with the seat count and the
   sign-up written in one DynamoDB transaction;
2. cancelling frees the seat, and the list of sign-ups comes page by page with an opaque
   cursor, never with a scan;
3. an `Idempotency-Key` header makes retries safe: the result is stored in the same
   transaction, replayed for a retry and forgotten after a day with DynamoDB TTL;
4. `template.yaml`, the SAM template: the HTTP API with throttling, the function, the table
   that survives the stack, a log group with retention, and a policy with exactly the
   actions the handler uses.

The fixed pieces:

- `src/signups.py` exposes `handler(event, context)`, a Lambda handler for **HTTP API
  payload format 2.0** events (`routeKey`, `pathParameters`, `queryStringParameters`,
  `headers` in lower case, `body` as text). It answers
  `{"statusCode": ..., "headers": {"content-type": "application/json"}, "body": "<JSON>"}`
  (no body for 204).
- The table's name comes from the environment variable `TABLE_NAME`. Create the client
  once per execution environment, at module level, with `boto3.client("dynamodb")`.
- The table has a string partition key `pk` and a string sort key `sk`. An event is the
  item `EVENT#<id>` / `META`; a sign-up is `EVENT#<id>` / `SIGNUP#<email>`. Your other
  attributes are your choice.

How it is checked, with no AWS account: the tests build HTTP API events and call your
handler inside **moto**, an in-memory DynamoDB that checks condition expressions,
transactions and reserved words like the real one; every request your code sends is
recorded. In stage 4, **cfn-lint** validates the SAM template (with the transform), the tests
compare its routes with the ones your handler answers, and its policy is resolved and run
through the **dojo's IAM policy evaluator**.

With an AWS account (not needed here):

```powershell
sam build; sam deploy --guided
```

When you are done, `[A]` copies a review prompt: ask an AI about your handler and template.
