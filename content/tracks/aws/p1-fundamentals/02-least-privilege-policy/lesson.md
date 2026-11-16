# Least Privilege: Tighten a Role's Policies

The invoice service runs as an ECS task with the role `invoice-service`. Someone made it
"just work" with `s3:*`, `dynamodb:*` and `sqs:*` on every resource, and a trust policy that
lets any AWS principal assume the role. Your job is to cut both down to what the service
really does, without breaking it.

The dojo checks your two JSON files with its policy evaluator: a list of requests the
service makes every day must still be **allowed**, and a list of requests it must never
make (deleting invoices, reading payroll, scanning the table, other accounts assuming the
role) must be **denied**. It follows AWS's documented evaluation logic; with an account
you would confirm the same thing with the IAM policy simulator or
`aws iam simulate-custom-policy`.

The **confused deputy** problem is why the trust policy needs conditions: a service
principal such as `ecs-tasks.amazonaws.com` acts for every AWS customer, so
`aws:SourceAccount` and `aws:SourceArn` make sure it assumes your role only on behalf of
your own account.
