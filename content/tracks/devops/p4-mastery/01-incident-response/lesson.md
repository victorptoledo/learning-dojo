# Incident Response: On-call, Roles, Communication and Postmortems

Systems fail; what separates good teams is how quickly they notice, how calmly they recover
and how much they learn. **Incident response** is a practised routine, not heroics.

## What an incident is

An **incident** is an unplanned event that degrades a service for its users (or threatens to),
and needs a coordinated response. Declare one early: it is cheap to close an incident that
turned out small, and expensive to coordinate one that nobody declared.

| Severity | Example | Response |
| --- | --- | --- |
| **SEV1** | checkout down for everyone, data loss, a security breach | page now, incident commander, status page, executives informed |
| **SEV2** | a major feature degraded, or one region down | page now, incident commander |
| **SEV3** | a minor feature broken, a workaround exists | business hours, a ticket |

Define the levels in advance, in user terms, so nobody argues about them at 3 a.m.

## On-call

- A **rotation** with a primary and a secondary, handed over with notes on open issues.
- **Paging** only for actionable, user-facing symptoms (previous lessons); everything else waits.
- **Runbooks** linked from every alert.
- A sustainable load: few pages per shift, compensation or time off, and a team that fixes
  the causes of repeated pages.

## Roles

Borrowed from emergency services (the Incident Command System), the roles keep one person from
doing everything:

| Role | Does | Does not |
| --- | --- | --- |
| **Incident commander (IC)** | owns the incident: sets priorities, assigns work, decides, calls for help | debug or type commands |
| **Operations lead** | investigates and changes the system, with helpers | talk to stakeholders |
| **Communications lead** | writes status updates for users and internal stakeholders on a schedule | change the system |
| **Scribe** | keeps the timeline: what was seen, tried and decided, with times | |

In a small team one person may hold two roles, but the IC should not be the one deep in the
logs. Work in one dedicated channel (`#inc-2026-11-15-checkout`), and hand over roles
explicitly when someone is tired.

## Mitigate first

The first goal is to **stop the user impact**, not to understand it. Mitigations, roughly in
order of preference:

1. **roll back** the most recent change (deploy, configuration, feature flag, infrastructure);
2. **fail over** to another zone or region, or drain the bad hosts;
3. **scale up** or shed load (rate limits, disable expensive features);
4. **restart** as a last resort (it often destroys evidence).

"What changed?" is the most useful question: most incidents follow a change. Keep evidence
(logs, dumps, a copy of the bad config) before you destroy it, but do not let evidence-gathering
delay mitigation. Root cause analysis comes after.

## Communication

- Post updates on a **schedule** (every 30 minutes for SEV1), even if the update is "still
  investigating, next update at 15:30".
- Say what users see, what you are doing and when the next update comes; avoid internal jargon
  and guesses about causes.
- Use a **status page** for customers and a stakeholder channel internally.

## Postmortems

After every significant incident, write a **postmortem** (also called an incident review) within
a few days, while memories are fresh:

- **summary** and **impact** in user terms (who, how long, how badly);
- **timeline** with times: trigger, detection, response, mitigation, resolution;
- **contributing factors**: complex systems rarely have one root cause; ask "how did this make
  sense at the time?" rather than "who did it?";
- **what went well**, and where you got lucky;
- **action items**: specific, owned, prioritised, tracked to completion.

**Blameless** does not mean nobody is accountable; it means people are not punished for
honest mistakes, so they tell the whole story. Punishment teaches people to hide information,
and hidden information causes the next outage. Track the metrics that improve with practice:
**time to detect**, **time to mitigate** and how many incidents repeat.

## Practise

Run **game days** and tabletop exercises: simulate an incident (a zone failure, an expired
certificate, a deleted database) and walk through the response. People who have done it in
practice stay calm when it is real.

## Summary

- Declare incidents early, with severities defined in user terms.
- On-call needs actionable pages, runbooks and a sustainable rotation.
- Separate roles: an incident commander who coordinates, an operations lead who fixes, a
  communications lead who informs, a scribe who records.
- Mitigate first (roll back what changed), diagnose later; update stakeholders on a schedule.
- Blameless postmortems find contributing factors and produce tracked action items.

## Further reading

- *Site Reliability Engineering*, chapter 14 "Managing Incidents": <https://sre.google/sre-book/managing-incidents/>
- *Site Reliability Engineering*, chapter 15 "Postmortem Culture": <https://sre.google/sre-book/postmortem-culture/>
- PagerDuty Incident Response documentation: <https://response.pagerduty.com/>
- Atlassian incident management handbook: <https://www.atlassian.com/incident-management/handbook>
- John Allspaw, "Blameless PostMortems and a Just Culture": <https://www.etsy.com/codeascraft/blameless-postmortems>
