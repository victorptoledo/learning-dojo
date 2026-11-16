---
from: java
status: draft
concept: "Bounded contexts and context maps"
from_way: "One `com.company.domain` package with a `Customer` entity used by every feature, and generated classes from a partner's WSDL used directly in business code."
native_way: "One top-level package or module per bounded context with its own model classes, translation at the edges (an ACL package that maps generated client classes into the context's records), and ids shared instead of entities."
sensei_tip: "Classes generated from someone else's OpenAPI or WSDL belong to their model. Keep them inside an `acl` or `adapter` package and map them; never let them appear in your domain's method signatures."
---

| Java | Strategic DDD idea |
| --- | --- |
| a package or Maven module per context | bounded context |
| records named in the experts' terms | ubiquitous language in code |
| a mapper over a generated client | anti-corruption layer |
| an OpenAPI spec you publish | open host service with published language |
| a tiny shared module (`Money`, ids) | shared kernel |
| ArchUnit: only `..acl..` may use the client | the ACL enforced |
