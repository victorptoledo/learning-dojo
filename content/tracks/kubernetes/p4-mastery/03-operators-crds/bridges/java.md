---
from: java
status: draft
concept: "Your own declarative APIs"
from_way: "An internal admin service with a REST API and a scheduled job that provisions databases for teams."
native_way: "A CRD makes the request a Kubernetes object, and an operator reconciles it like any built-in controller, with RBAC, watches and kubectl for free."
sensei_tip: "The Java Operator SDK (with Quarkus or Spring Boot) gives you a Reconciler interface with dependent resources; fabric8's client generates model classes from your CRD."
---

| Java world | Kubernetes |
| --- | --- |
| a JPA entity for a provisioning request | a custom resource |
| a REST controller for it | the API server serving the CRD |
| a Quartz job that provisions | a controller's reconcile loop |
| Bean Validation annotations | the CRD's OpenAPI schema and CEL rules |
