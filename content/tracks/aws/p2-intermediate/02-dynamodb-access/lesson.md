# DynamoDB Access Patterns with boto3

In DynamoDB you design the keys from the questions the application asks, then each
question is one `GetItem` or one `Query`. This kata gives you the design and asks you to
write the repository that uses it, with the low-level boto3 client so you see DynamoDB's
own typed JSON (`{"S": "..."}`, `{"N": "..."}`).

| Access pattern | Call | Key condition |
| --- | --- | --- |
| store a new order, never overwrite | `PutItem` | `attribute_not_exists(pk)` |
| read one order | `GetItem` | `pk`, `sk` |
| a customer's orders, newest first | `Query` | `pk = CUSTOMER#…`, `begins_with(sk, ORDER#)`, descending |
| every order in a status | `Query` on the GSI `by-status` | `gsi1pk = STATUS#…` |
| ship an order that is placed | `UpdateItem` | condition `#status = PLACED` |

The tests run your code against **moto**, an in-memory implementation of the DynamoDB API
inside the test process: the same boto3 calls, the same errors, no network and no account.
