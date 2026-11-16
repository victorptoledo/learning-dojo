# Policy as Code: Security Rules in cfn-guard

Detective controls (AWS Config, Security Hub) find a public bucket after it exists.
**Preventive** controls stop it before deployment: rules that every template must pass in
CI, and, with CloudFormation **Guard Hooks**, at deploy time in the account itself.

**AWS CloudFormation Guard** (cfn-guard) is AWS's open-source policy language for JSON and
YAML documents. You select resources, filter them, and state clauses that must hold:

```text
let tables = Resources.*[ Type == 'AWS::DynamoDB::Table' ]

rule tables_have_point_in_time_recovery when %tables !empty {
    %tables.Properties.PointInTimeRecoverySpecification.PointInTimeRecoveryEnabled == true
    <<DynamoDB tables keep point-in-time recovery on>>
}
```

Good rules are **tested like code**: a compliant template must pass, and each known
mistake must fail exactly the rule meant to catch it, no more. That is how this kata
checks yours, with one set of fixtures you can see and one set you cannot.
