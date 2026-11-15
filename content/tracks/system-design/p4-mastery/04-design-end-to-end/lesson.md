# Designing a System End to End

This last lesson puts the whole track together. It walks the design process from the first
lesson through two complete examples, then gives you a checklist for design reviews and
interviews. Read the examples actively: before each step, decide what you would do, then
compare.

## The process, again

1. **Requirements**: functional, non-functional as numbers, what is out of scope.
2. **Estimates**: traffic, storage, bandwidth, the dominant load.
3. **API and data model**.
4. **High-level design**: the path of the main requests.
5. **Deep dives**: the hardest one or two parts.
6. **Failure and scale**: what breaks at 10×, when a zone or a dependency dies; how it is
   observed.

## Example 1: a chat service

**Requirements.** One-to-one and group chats (up to 500 members), delivery and read receipts,
history on every device, online presence. 50 M daily users, each sending 40 messages a day.
Messages must never be lost once the sender sees "sent"; delivery to online recipients in under
500 ms at p99.

**Estimates.**

```text
messages:  50 M × 40 / 86,400 ≈ 23,000/s average, ~70,000/s peak
fan-out:   average 3 recipients (groups) → ~210,000 deliveries/s at peak
storage:   2 B messages/day × 300 bytes ≈ 600 GB/day ≈ 220 TB/year, ×3 replicas
connections: ~15 M concurrently online → at 100,000 connections per gateway host, ~150 hosts plus spares
```

**API.** A persistent WebSocket per device for sending and receiving; HTTP for history
(`GET /chats/{id}/messages?before=cursor`) and media upload (pre-signed object storage URLs).

**Data model.** Messages partitioned by `chat_id`, ordered by a per-chat sequence number:
`(chat_id, seq) → sender, body, sent_at`. A wide-column or LSM store fits (write-heavy, range
reads by chat), as Discord's migration to ScyllaDB shows. Per-user "inbox" of chats with the
last read `seq` for receipts and unread counts.

**High-level design.**

```text
device ──WebSocket──► gateway (stateful, holds connections) ──► chat service
                         ▲                                      │ 1. assign seq (per chat) and persist (quorum write)
                         │                                      │ 2. ack "sent" to the sender
     presence/registry: user → gateway (in a replicated KV)     │ 3. publish to the chat's partition in a log
                         │                                      ▼
                         └──────── delivery workers ◄──── log partitioned by chat_id
                                   look up recipients' gateways, push; offline → push notification
```

**Deep dives.**

- *Ordering*: a per-chat sequence (assigned by the chat's partition leader) gives every member
  the same order; the log partitioned by `chat_id` keeps delivery in that order.
- *Exactly-once to the user*: clients send a client-generated message id; the service
  deduplicates retries, and devices dedupe by `(chat_id, seq)` on reconnect, asking for
  everything after their last `seq`.
- *Gateways* are stateful: a gateway crash drops its connections; clients reconnect (with jitter
  to avoid a stampede) to another gateway and resync from their last `seq`.
- *Big groups*: fan-out per message equals the group size; cap group size, or for very large
  "channels" switch to pull (members fetch on open), the same hybrid as feeds.

**Failure and scale.** A zone loss drops a third of the connections, which reconnect elsewhere
(gateways sized N+1 zones). Storage uses quorum writes across zones. The log absorbs delivery
backlogs; alerts on consumer lag. SLOs: 99.99 % of sends acknowledged; p99 delivery < 500 ms.

## Example 2: a flash ticket sale

**Requirements.** 50,000 seats for a concert go on sale at 10:00; 2 M people try at once. No seat
sold twice; a buyer holds a seat for 10 minutes while paying; the site must not fall over; a fair
order of access is desirable.

**Estimates.** 2 M users refreshing every few seconds is ~500,000 requests/s for a few minutes,
for a system whose normal load is 2,000/s. Only 50,000 purchases can ever succeed.

**Key insight.** The demand is ~40 times the supply, so most of the work is saying "not yet" or
"sold out" cheaply. Protect the core (inventory and payments) with **admission control**.

**Design.**

```text
CDN (static event page, cached seat-map snapshot every few seconds)
  → virtual waiting room: users get a signed, ordered token; admitted at the rate the core can serve (e.g. 2,000/min)
  → reservation service (only admitted tokens): hold seat = conditional write
        UPDATE seats SET held_by = ?, held_until = now() + 10 min
        WHERE event = ? AND seat = ? AND (held_by IS NULL OR held_until < now())
  → payment with an idempotency key per hold; on success mark sold; on expiry the seat returns
```

**Deep dives.**

- *No double sale*: the conditional write on a single, strongly consistent partition per event
  (or per section, for parallelism) is the linearizable step. Everything else can be eventually
  consistent (the seat map may be a few seconds stale; the reservation tells the truth).
- *Hot partition*: one event is one hot key; split the inventory by section so writes spread over
  several partitions, each still strongly consistent.
- *Overload*: the waiting room is load shedding with fairness; the core never sees more than it
  can serve. Retries from clients are absorbed by the CDN and the waiting room, not the database.
- *Expired holds* are released by a sweeper (or lazily, by the condition `held_until < now()`
  in the reservation itself, which needs no background job at all).

**Failure and scale.** If payments slow down, holds expire and seats return; the waiting room
slows admission based on the core's latency (back-pressure). Bots: rate limits per account and
device, and proof of work or a challenge at the waiting room entry.

## A design review checklist

| Area | Questions |
| --- | --- |
| Requirements | Are the SLOs, the scale and the consistency needs written as numbers? What is out of scope? |
| Data | What is the source of truth for each entity? The partition key? Where is strong consistency needed, and how is it provided? |
| Reads | What is cached, where, for how long, and how is it invalidated? Read-your-writes? |
| Writes | Idempotency keys? What happens on retry, duplicate, or partial failure? |
| Async | Which work is queued? Ordering key? Dead letters? Consumer lag alerts? |
| Overload | Timeouts, retry budgets, bounded queues, load shedding by priority? |
| Failure | What happens when a zone, a dependency, the cache or the leader fails? Blast radius? |
| Operations | SLIs and dashboards, alerts on burn rate, runbooks, staged rollouts, backups tested? |
| Cost | Cost per request and per GB; the largest line item; what grows fastest? |
| Evolution | How do schemas, APIs and partitions change without downtime? |

Write the important choices down as short **architecture decision records**: context, the
decision, the alternatives considered and the consequences. Future you (and the next team) will
need the *why*, not just the diagram.

## In an interview

- Spend the first minutes on requirements and numbers; interviewers reward clarifying questions.
- Draw the simple version first, end to end, then improve the part the numbers point at.
- Say the trade-off aloud every time you choose ("a cache here makes reads fast, but profiles
  can be 60 s stale, which is acceptable because ...").
- Go deep on one or two components rather than shallow on ten.
- Close with failure modes, monitoring and what you would do next with more time.

## Summary

- The same six steps work for any system; numbers decide where to spend design effort.
- Chat: persistent connections on stateful gateways, per-chat ordering with sequences, a log
  partitioned by chat, resync from the last sequence.
- Flash sale: shed demand at the edge and in a waiting room; one linearizable conditional write
  per seat; holds that expire on their own.
- Review designs with a checklist and record decisions as ADRs.

## Further reading

- Discord engineering, "How Discord Stores Trillions of Messages": <https://discord.com/blog/how-discord-stores-trillions-of-messages>
- Stripe engineering, "Designing robust and predictable APIs with idempotency": <https://stripe.com/blog/idempotency>
- AWS Well-Architected Framework (pillars and design principles): <https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html>
- High Scalability (case studies of real architectures): <https://highscalability.com/>
- The System Design Primer, worked design questions: <https://github.com/donnemartin/system-design-primer>
- ByteByteGo, System Design 101 (visual summaries): <https://github.com/ByteByteGoHq/system-design-101>
