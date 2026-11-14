# Engineering Leadership

Senior backend engineers are measured less by the code they write and more by the decisions they
help a team make, the systems they keep healthy and the people they grow. This lesson covers the
habits that make that work: writing decisions down, reviewing well, owning production, managing
technical debt and multiplying others.

## Design documents

Before a significant change (a new service, a schema redesign, a migration), write a short design
doc and get it reviewed:

1. **Context and problem**: what is wrong or missing, and why now.
2. **Goals and non-goals**: what success looks like, and what is explicitly out of scope.
3. **Proposal**: the design, with diagrams, APIs, data model and how it fails.
4. **Alternatives considered**, and why they lost.
5. **Risks, rollout and rollback**, migration plan, monitoring.
6. **Open questions**.

The doc is a tool for **thinking and agreement**, not paperwork: a page or two that reviewers can
challenge is worth more than ten that nobody reads. Decisions that outlive the doc go into **ADRs**.

## Code review

Reviews spread knowledge and catch problems early. Make them useful:

- **Small changes** (a few hundred lines) get real reviews; huge ones get rubber stamps.
- As an author: explain the why in the description, point at the tricky parts, add tests.
- As a reviewer, in order of importance: correctness and safety (security, data loss, concurrency),
  design and clarity, tests, then style (which a formatter and linters should handle).
- Separate **blocking** issues from suggestions ("nit:", "optional:"); ask questions instead of
  issuing orders; approve when it is better than before, not when it is how you would have written it.
- Review promptly: a day of waiting is a day of lost flow for the author.

## Owning production: on-call

You build it, you run it. Healthy on-call:

- A rotation shared by the team, with a secondary, and handover notes.
- **Runbooks** for every alert; alerts that are actionable (see the SRE lesson).
- Time after a rough night to recover, and the next sprint's capacity reserved to fix what paged.
- Track pages per shift: more than a couple a week means the system or the alerts need work.

## Technical debt

Debt is the gap between the code you have and the code you would want, given what you know now.
Some is deliberate (ship now, clean up after the launch), some accidental. Manage it like money:

- **Make it visible**: a list with the cost of each item in terms the business understands ("every
  new payment method takes two weeks because...").
- **Pay it where you work**: improve code you touch for a feature (the boy-scout rule), and plan
  bigger paydowns explicitly.
- **Prioritise by interest**: debt in code that changes often costs every week; debt in stable code
  can wait.
- Do not ask for a "refactoring quarter"; tie paydowns to outcomes: faster delivery, fewer incidents.

## Multiplying others

- **Mentoring**: pair on hard problems, review with explanations, give people work that stretches
  them, and let them own it.
- **Sharing context**: write things down (docs, ADRs, postmortems), give short talks, keep the
  onboarding guide current.
- **Making good decisions easy**: templates, linters, libraries and paved paths so the right way is
  the easy way.
- **Disagree and commit**: argue with data before a decision, support it fully after.

## Communicating with the business

Translate technical concerns into risk, cost and time: "without this migration, Black Friday traffic
will exceed the database's capacity by about 30%", not "we need to shard". Give options with
trade-offs, a recommendation, and what you need to decide.

## Summary

- Write short design docs (problem, goals, proposal, alternatives, risks) and record decisions in ADRs.
- Keep reviews small and prompt; review correctness and safety first; label nits.
- Own production with a fair on-call, runbooks and time to fix what pages.
- Make technical debt visible, pay it where you work, prioritise by interest, tie it to outcomes.
- Multiply the team through mentoring, written context and paved paths; speak the business's language.
