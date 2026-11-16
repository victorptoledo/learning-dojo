# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a car rental whose booking code speaks the
   language of a 1998 CRM and writes straight into billing's ledger. You give booking its own
   customer model behind an **anti-corruption layer**, and make billing a context that booking
   reaches only through its published api.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| The Modular Monolith | What does each module publish? What would make it hard to extract later? |
| Module Boundaries (kata) | How do I state the boundary so a build can check it? |
| Microservices and Their Costs | Which problem would splitting solve, and which new ones would it bring? |
| Strategic DDD | Where does this word change meaning? Upstream or downstream? Conformist or ACL? |
| Extract a Bounded Context (kata) | What does each context really need to know? What is shared: data or identity? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
