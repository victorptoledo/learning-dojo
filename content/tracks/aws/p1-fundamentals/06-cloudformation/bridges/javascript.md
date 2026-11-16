---
from: javascript
status: draft
concept: "Declare the infrastructure and let the engine converge it"
from_way: "Infrastructure is created by hand in the console, or by a Serverless Framework file nobody reviews."
native_way: "Templates or a TypeScript CDK app describe every resource; CloudFormation applies them as one stack."
sensei_tip: "TypeScript is the CDK's first language: type errors catch wrong property names before `cdk synth`, and constructs are just npm packages you can share."
---

| Node.js world | AWS |
| --- | --- |
| `serverless.yml` | SAM or a CDK stack |
| `npm run deploy` | `cdk deploy` or `aws cloudformation deploy` |
| a reusable React component | a CDK construct |
| `.env.production` | stack parameters and SSM parameters |
