# An Idempotent SQS Worker in Lambda

SQS delivers every message **at least once**: a worker that crashes after writing but
before the message is deleted sees it again, and standard queues may deliver a duplicate
on their own. A payment worker that adds money twice is a bug you cannot undo, so the
effect must be **idempotent**.

The pattern in this kata: write an idempotency marker (`PAYMENT#<id>`) with
`attribute_not_exists(pk)` **in the same DynamoDB transaction** as the balance update.
Either both happen once, or the transaction is cancelled and the duplicate is ignored.
(Powertools for AWS Lambda offers a ready-made `@idempotent` decorator built on the same
idea.)

The second pattern is **partial batch responses**. Lambda gives your function up to ten
SQS messages at once. If the handler raises, the whole batch returns to the queue and the
good messages are processed again. With `ReportBatchItemFailures` turned on in the event
source mapping, the handler returns `{"batchItemFailures": [{"itemIdentifier": "<messageId>"}]}`
and only those messages are retried; after `maxReceiveCount` attempts they move to the
dead-letter queue.
