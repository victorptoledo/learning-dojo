# The Security Mindset: CIA, Risk and the Defender's Role

Developers ask "does it work?". Security asks "how can it be **made** to do something it
should not?". This track is about the defender's side of that question: writing, configuring
and running software so that the answer is "not easily, and we would notice".

## What we protect: the CIA triad

| Property | Means | Broken by |
| --- | --- | --- |
| **Confidentiality** | only the right people can read the data | a leaked database dump, an API returning another user's orders |
| **Integrity** | data and code change only in authorised ways | a tampered price in a request, a malicious package update |
| **Availability** | the service works when it is needed | ransomware, a flood of requests, an expired certificate |

Two more properties often join them: **authenticity** (you know who sent it) and
**non-repudiation** (the sender cannot deny it later). Digital signatures, which you meet in
phase 2, give both.

## Assets, threats, vulnerabilities, risk

- An **asset** is something worth protecting: customer data, password hashes, signing keys,
  the payment flow, uptime.
- A **threat** is who or what could harm it: a criminal after card data, a curious insider, a
  bot trying leaked passwords, a careless deploy.
- A **vulnerability** is the weakness a threat uses: a query built by string concatenation, a
  default admin password, a storage bucket readable by anyone.
- **Risk** combines them: *likelihood × impact*. A vulnerability nobody can reach is a low
  risk; a trivial one on the login page is high.

You never remove all risk. You **reduce** it (add a control), **transfer** it (insurance, a
managed service), **avoid** it (do not collect the data at all) or **accept** it, on purpose
and written down.

## The principles every control comes from

| Principle | In practice |
| --- | --- |
| **Least privilege** | the app's database user can read and write its tables, not drop them; a CI token can push one repository |
| **Defense in depth** | parameterised queries *and* input validation *and* a low-privilege DB user *and* alerts |
| **Secure defaults** | new accounts have no admin rights; new buckets are private; debug mode is off |
| **Fail closed** | if the permission check throws, deny; if the signature check cannot run, reject |
| **Small attack surface** | close unused ports, remove unused endpoints and dependencies |
| **Separation of duties** | the person who writes a payment change is not the only one who approves it |
| **No security through obscurity** | assume attackers read your code (Kerckhoffs's principle); only keys are secret |

> [!TIP]
> When you review any design, ask the four questions of the Threat Modeling Manifesto: *What
> are we working on? What can go wrong? What are we going to do about it? Did we do a good
> enough job?* Phase 4 turns them into a method (STRIDE).

## The roles

Security work splits into overlapping roles: **application security** (secure code, reviews,
tooling in CI), **security engineering** (identity, secrets, infrastructure), the **SOC** and
**incident response** (detect and handle attacks), **GRC** (governance, risk and compliance)
and **offensive** testing (penetration testers and red teams who attack *with permission*).
Every developer is part of it: most vulnerabilities are written, and fixed, by developers.

## Legal and ethical limits

Testing a system you do not own, or that you have not been **explicitly authorised in
writing** to test, is a crime in most countries (for example the US Computer Fraud and Abuse
Act, the UK Computer Misuse Act, Brazil's Lei 12.737/2012). Authorised tests have a written
**scope** (which hosts, which techniques, which dates) and **rules of engagement**. If you
find a vulnerability by accident, report it through the vendor's disclosure policy (often
published at `/.well-known/security.txt`, RFC 9116) and do not use it.

Every kata in this dojo is **defensive and offline**: you fix, validate, detect and harden,
on data inside a container with no network.

## Summary

- Security protects the confidentiality, integrity and availability of assets.
- Risk = likelihood × impact; reduce, transfer, avoid or knowingly accept it.
- Least privilege, defense in depth, secure defaults and fail closed are where controls come from.
- Only test what you are authorised in writing to test; report what you find responsibly.

## Further reading

- NIST Cybersecurity Framework 2.0: <https://www.nist.gov/cyberframework>
- NIST SP 800-30 Rev. 1, *Guide for Conducting Risk Assessments*: <https://csrc.nist.gov/pubs/sp/800/30/r1/final>
- OWASP Developer Guide, security principles: <https://owasp.org/www-project-developer-guide/>
- Threat Modeling Manifesto: <https://www.threatmodelingmanifesto.org/>
- RFC 9116, *A File Format to Aid in Security Vulnerability Disclosure*: <https://www.rfc-editor.org/rfc/rfc9116>
