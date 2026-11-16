---
from: java
status: draft
concept: "Declare the infrastructure and let the engine converge it"
from_way: "Ops runbooks and console clicks create environments that slowly drift apart."
native_way: "CloudFormation templates (or CDK apps in Java) declare the resources; change sets preview each update."
sensei_tip: "Write the CDK in Java with `software.amazon.awscdk` and unit-test the stack with `Template.fromStack(stack).hasResourceProperties(...)`, just like a JUnit test."
---

| Java world | AWS |
| --- | --- |
| a Maven multi-module build | a CDK app with one stack per lifecycle |
| Flyway migrations | change sets reviewed before execution |
| a config class per profile | parameters, mappings and conditions |
| JUnit | CDK assertions on the synthesised template |
