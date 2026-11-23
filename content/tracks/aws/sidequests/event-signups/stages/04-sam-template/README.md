The handler works; now describe how it runs. This stage added `template.yaml`, a SAM
template with the function and nothing else. Finish it:

| Resource | Rule |
| --- | --- |
| `SignupsApi` (`AWS::Serverless::HttpApi`) | throttled for every route: `DefaultRouteSettings` with `ThrottlingBurstLimit` at most 100 and `ThrottlingRateLimit` at most 50 (a sign-up rush must not run up the bill or the table) |
| `SignupsFunction` | `CodeUri: src/`, `Handler: signups.handler`, `Runtime: python3.13`, `Architectures: [arm64]` (Graviton, cheaper per millisecond), a `Timeout` of at most 10 seconds; `TABLE_NAME` in its environment, `!Ref SignupsTable` |
| routes | one `HttpApi` event per route the handler answers, all with `ApiId: !Ref SignupsApi`, and no other: the tests call your handler with every route of the template |
| permissions | `Policies` with **one inline statement** (no SAM policy templates such as `DynamoDBCrudPolicy`, which grant `Scan`, batch writes and more): exactly the DynamoDB actions the handler uses, on `!GetAtt SignupsTable.Arn` only |
| `SignupsLogGroup` (`AWS::Logs::LogGroup`) | `LogGroupName: !Sub "/aws/lambda/${SignupsFunction}"` and `RetentionInDays: 30`. Lambda would create the group itself, but with logs kept forever |
| `SignupsTable` (`AWS::DynamoDB::Table`) | on demand (`PAY_PER_REQUEST`); `pk` and `sk` strings; **TTL** on `expires_at`; point-in-time recovery; encryption (`SSESpecification`); `DeletionPolicy` and `UpdateReplacePolicy` `Retain` |

Globals are welcome: settings shared by every function may sit under `Globals: Function:`;
the tests read them as SAM does.

The tests resolve the statement and ask the IAM evaluator what the function may do. Think
about what `transact_write_items` needs: there is **no** IAM action called
`TransactWriteItems`; each operation inside the transaction is authorized as its own action
(`Update` as `dynamodb:UpdateItem`, `Put` as `dynamodb:PutItem`, `Delete` as
`dynamodb:DeleteItem`, `ConditionCheck` as `dynamodb:ConditionCheckItem`).

> [!TIP]
> In SAM, `Policies` is a list. Each item is a SAM policy template, the name or ARN of a
> managed policy, or an inline document: `- Statement: [ {Effect, Action, Resource} ]`.
