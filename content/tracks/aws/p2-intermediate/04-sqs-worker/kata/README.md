# An Idempotent SQS Worker in Lambda

Finish `handler.py`: a Lambda function behind an SQS queue of payments. The table name is
in the environment variable `TABLE_NAME`; `tests/conftest.py` creates it in moto with the
key `pk`.

| Function | Rule |
| --- | --- |
| `parse_payment(body)` | returns `{"payment_id", "account", "amount_cents"}` from the JSON body; raises `ValueError` when the body is not a JSON object, `payment_id` or `account` is not a non-empty string, or `amount_cents` is not a positive integer (not a float, string or boolean) |
| `apply_payment(payment)` | **one** `transact_write_items` call: put the marker `pk = PAYMENT#<payment_id>` only if it does not exist, and `ADD` the amount to `balance_cents` of `pk = ACCOUNT#<account>`; a cancelled transaction because the marker exists is a duplicate and returns quietly; other errors propagate |
| `handle(event, context)` | processes every record of `event["Records"]` and returns `{"batchItemFailures": [...]}` with the `messageId` of each record that was invalid or hit an AWS error; one failure never stops the other records |

Also keep using `dynamodb()`: it creates the boto3 client once per execution environment,
so warm invocations reuse its connections.

In the SAM template (the next checkpoint) the function would be wired like this:

```yaml
Events:
  Payments:
    Type: SQS
    Properties:
      Queue: !GetAtt PaymentsQueue.Arn
      BatchSize: 10
      FunctionResponseTypes: [ReportBatchItemFailures]
```

How the check works: pytest runs your handler with SQS events built like the ones Lambda
sends, against moto's in-memory DynamoDB; ruff checks the module.

There are 19 tests: 1 in `tests/test_visible.py`, 17 hidden ones and the ruff check.
