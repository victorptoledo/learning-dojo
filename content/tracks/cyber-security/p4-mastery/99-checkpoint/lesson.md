# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards threat
   modeling, incident response and running a security program. You need 7 right.
2. **The integration kata** (press **Enter**): a post-incident review kit. Threat model the
   service with STRIDE per element, prove the evidence was not changed with hashes and a
   hash-chained custody log, rebuild the timeline in UTC with the notification deadline,
   and turn the vulnerability feed into a remediation plan with SLAs and exceptions.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Threat modeling with STRIDE | Which STRIDE threats apply to an external entity, a process, a data store, a data flow? Where do threats cluster? |
| Incident response | Why hash the evidence and keep a chain of custody? Why a UTC timeline? When does the notification clock start? |
| Security program | Why does KEV on an internet-facing system beat a CVSS 9.8 nobody can reach? What makes an exception safe? |
| Security logging (Advanced) | What makes a log tamper-evident? |
| Modern cryptography (Intermediate) | Which hash for integrity, and why not MD5? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
