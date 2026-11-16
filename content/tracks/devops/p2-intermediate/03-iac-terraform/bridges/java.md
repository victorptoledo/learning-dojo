---
from: java
status: draft
concept: "Infrastructure has a build file and a dependency graph"
from_way: "Maven resolves a dependency graph from pom.xml and builds modules in order."
native_way: "Terraform builds a graph from references between resources and creates them in order, in parallel where it can; modules are reusable like Maven modules."
sensei_tip: "Treat provider versions like dependency versions: pin with ~>, commit the lock file, upgrade deliberately."
---

| Java world | Terraform |
| --- | --- |
| `pom.xml` | `.tf` files |
| a Maven module | a Terraform module |
| `dependencyManagement` versions | `required_providers` |
| `mvn verify` | `terraform validate` and `terraform test` |
