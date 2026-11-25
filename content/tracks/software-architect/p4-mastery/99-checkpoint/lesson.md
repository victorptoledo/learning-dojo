# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions, weighted towards evolutionary architecture,
   fitness functions, documentation and governance, with the advanced patterns they rely on.
   You need 7 right.
2. **The integration kata** (press **Enter**): a 2009 pricing engine that every invoice depends
   on. You **strangle** it instead of rewriting it: a port and a legacy adapter, a modern
   implementation, a strangler that runs both in **shadow** before each category cuts over,
   **fitness functions with a ratchet** to keep the new shape, and a **decision record** that
   explains why.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Evolutionary Architecture | Which characteristic must not degrade, and which fitness function protects it? How do I move from old to new in steps that each ship? |
| A Fitness Function Suite (kata) | Does the build fail on a new violation and still let the team work with the old ones? |
| Documenting Architecture | Which audience is this diagram for, and which level of C4 answers its question? What goes in arc42 instead? |
| The Architect's Role | Who decides, and who must be consulted? Which trade-off point does this decision sit on? |

The kata also leans on the earlier phases:

| Phase | What you use from it |
| --- | --- |
| First Steps | a decision record with context, decision and consequences |
| Fundamentals | a port that points inwards, an adapter outside, and a composition root that wires them |
| Intermediate | an anti-corruption layer: the engine's codes never leave the adapter |
| Advanced | at-least-once thinking: a shadow run that fails must not fail the customer |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
