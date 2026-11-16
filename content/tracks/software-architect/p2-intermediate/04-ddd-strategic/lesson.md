# Strategic DDD

Eric Evans's *Domain-Driven Design* (2003) is remembered for entities and repositories, but its
author has said the most important part is the **strategic** one: how to split a large domain
into models that each stay consistent, and how those models relate. That is architecture.

## Subdomains: where to invest

A business is made of **subdomains**, areas of what it does:

| Kind | What it is | What to do |
| --- | --- | --- |
| **core** | what makes this business different and competitive | build it yourself, with your best people, and model it carefully |
| **supporting** | needed and specific to you, but not a differentiator | build it simply, or have it built |
| **generic** | the same for everyone (payroll, email, identity) | buy it or use an open-source product |

For a ride-hailing company, matching drivers to riders and pricing are core; driver onboarding is
supporting; payments processing and notifications are generic. Spending months modelling the
generic parts is waste; buying a product for the core gives away the advantage.

## Ubiquitous language

Within one model, developers and domain experts use **the same words with the same meaning**, in
conversation, in documents and **in the code**. If experts say "a policy lapses" and the code says
`setStatus(3)`, every conversation needs a translator, and translations hide bugs. When the
language is awkward to put in code, the model is probably wrong.

## Bounded contexts

The catch: a large business has no single consistent language. "Product" means a name, a
description and a price to the people who sell it, and a weight, a bin and a count to the people
who store it. "Customer" means a lead to marketing, an account with a credit limit to billing, a
delivery address to shipping. One `Product` class trying to mean everything becomes a god class
that every team edits.

A **bounded context** is the explicit boundary inside which one model and its language apply.
Inside it, words are precise; at its edge, they are translated. Each context has its own model
of "product", and they share **identity** (the sku), not the class. The *Extract a Bounded
Context* kata splits exactly such a shared class.

How to find the boundaries:

- listen for the same word meaning different things, or different words for one thing;
- look at who the experts are: different departments often mean different contexts;
- follow the business processes and see where responsibility is handed over;
- run an **Event Storming** workshop (Alberto Brandolini): stick domain events on a wall in time
  order with the experts; clusters and hand-offs show the contexts.

A bounded context is a linguistic and model boundary. It often becomes a module of a modular
monolith or a service, and ideally one team owns it, but the three need not map one to one.

## Context maps

A **context map** shows the contexts and how each pair relates. Relationships are about power and
translation as much as about code. Upstream (U) contexts influence downstream (D) ones.

| Pattern | What it means |
| --- | --- |
| **Partnership** | two teams succeed or fail together and plan their interfaces jointly |
| **Shared kernel** | a small, explicitly shared part of the model (and code) that both teams change only by agreement |
| **Customer / supplier** | upstream supplier, downstream customer; the customer's needs are planned into the supplier's work |
| **Conformist** | downstream adopts the upstream model as it is, because it has no influence (or it is good enough) |
| **Anti-corruption layer (ACL)** | downstream builds a translating layer so the upstream model does not leak into its own |
| **Open host service (OHS)** | upstream offers a well-defined protocol (an API) for any consumer |
| **Published language** | a documented shared exchange format (a schema, a standard such as HL7 or ISO 20022) |
| **Separate ways** | no integration; the contexts solve their needs independently |

Some practical rules:

- With a legacy or third-party system you cannot change, choose **conformist** if its model is
  acceptable and **ACL** if it would corrupt yours. The *Car Rental* checkpoint kata builds an ACL
  over a CRM's string maps.
- An upstream with many consumers should be an **open host service** with a **published
  language**, so it does not have to negotiate with each of them.
- Keep shared kernels small: they are coupling you chose.
- Big ball of mud is also on the map: draw a boundary around it and protect yourself with an ACL
  rather than pretending it is not there.

## Further reading

- Eric Evans, *Domain-Driven Design: Tackling Complexity in the Heart of Software* (Addison-Wesley, 2003), part IV
- Eric Evans, *Domain-Driven Design Reference* (2015): <https://www.domainlanguage.com/ddd/reference/>
- Martin Fowler, "Bounded Context": <https://martinfowler.com/bliki/BoundedContext.html>
- Vlad Khononov, *Learning Domain-Driven Design* (O'Reilly, 2021)
- DDD Crew, "Context Mapping": <https://github.com/ddd-crew/context-mapping>
- Alberto Brandolini, *Introducing EventStorming* (Leanpub)
