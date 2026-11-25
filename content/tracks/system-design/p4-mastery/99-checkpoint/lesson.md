# Checkpoint 4: Final Exam

This is the **final exam** of the System Design track. It closes the **Mastery** phase and has
two parts, and you need both:

1. **The quiz** (press **Q**): eight questions on consensus, leases, resilience and end-to-end
   design, some of them mixed with ideas from earlier phases. You need 7 right.
2. **The integration kata** (press **Enter**): the router of a **cell-based** service. Tenants
   go to **shuffle shards** chosen by rendezvous hashing. The cell map comes from a control plane
   and is only replaced by a newer, valid version, so the router stays up when the control plane
   is down (**static stability**). Health checks eject cells but stop trusting themselves when
   most cells look sick, and the router **sheds** work by priority instead of falling over.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Consensus, Leases and Fencing | What does a majority tolerate? Why is a lease not enough without a fencing token? |
| Fenced Lease (kata) | Who checks the token, and what happens to a writer that paused? |
| Resilience at Scale | What is my error budget? What keeps working when the control plane is gone? How big is the blast radius of one bad tenant? |
| Designing a System End to End | Requirements and estimates first? Which step must be linearizable, and what can be stale? |
| Load Balancers and Consistent Hashing (P1) | How do health checks and hashing decide where a request goes? |
| Overload and Back-Pressure (P2) | What do I shed first, and why is shedding better than queueing? |

> [!TIP]
> The kata reuses ideas from the whole track: hashing from P1, shedding from P2, failover inside
> a group from P3, and versions that behave like fencing tokens from P4.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
