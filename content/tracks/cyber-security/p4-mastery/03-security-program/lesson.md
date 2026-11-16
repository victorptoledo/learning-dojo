# Secure SDLC, Vulnerability Management and Security Leadership

Individual controls are not enough at scale. A **security program** decides what to protect
first, builds security into how software is made, measures whether it works and earns the
trust of the teams it depends on.

## Security in the development life cycle

NIST's **Secure Software Development Framework** (SSDF, SP 800-218) and **OWASP SAMM** describe
what mature teams do; in practice it maps onto each stage:

| Stage | Practice |
| --- | --- |
| Requirements | security and privacy requirements (OWASP ASVS levels), abuse cases |
| Design | threat modeling for risky features, secure defaults, reviewed patterns |
| Code | secure coding guidance, code review, **secret scanning** in pre-commit and push protection |
| Build | **SCA** (dependency scanning), **SAST** (static analysis such as CodeQL or Semgrep), IaC and container scanning, SBOM, signing |
| Test | **DAST** (dynamic scanning such as OWASP ZAP), security unit tests for authorisation rules, fuzzing for parsers |
| Release and run | hardened configuration, monitoring and detection, a vulnerability disclosure policy, bug bounty if mature |

Tools only help if developers trust them: start with few, high-signal rules; show findings
in the pull request where the code is fixed; **warn first, block later**, and only on what is
both serious and reliable (a leaked secret, a critical reachable dependency CVE). A
**security champions** network (one interested developer per team) multiplies a small
security team.

## Vulnerability management

Scanners produce more findings than anyone can fix, so prioritisation is the job:

| Signal | Tells you |
| --- | --- |
| **CVSS** (v4.0) | technical severity of a vulnerability, 0–10, in general |
| **EPSS** | the probability that it will be exploited in the next 30 days |
| **CISA KEV** | it **is** being exploited now |
| Your context | is the code reachable? is the system internet-facing? what data does it hold? |

A reachable, internet-facing KEV entry on the payment system beats a CVSS 9.8 in a test
dependency that never runs. Set **SLAs** by priority (for example: known exploited and exposed
within days, critical within 2 weeks, high within 30 days), give every asset an **owner**,
and track exceptions with expiry dates.

## Testing and disclosure

- **Penetration tests** and **red team** exercises are authorised attacks: a written scope,
  rules of engagement, dates and emergency contacts. They check assumptions; they are not a
  substitute for building security in.
- **Vulnerability disclosure**: publish how to report issues (`security.txt`, RFC 9116),
  answer quickly, fix, credit the reporter. **Coordinated disclosure** gives vendors time to
  fix before details are published (often 90 days). Bug bounties pay for findings once the
  basics are in place.

## Frameworks and compliance

| Framework | What it is |
| --- | --- |
| **NIST CSF 2.0** | outcomes for Govern, Identify, Protect, Detect, Respond, Recover: a map of the whole program |
| **ISO/IEC 27001:2022** | a certifiable *management system*: risk assessment, policies, and the Annex A controls |
| **SOC 2** | an auditor's report on controls, common for SaaS vendors |
| **PCI DSS v4.0** | mandatory controls for card data |
| **CIS Controls v8** | a prioritised list of technical safeguards (implementation groups) |
| GDPR, LGPD | privacy law: lawful basis, minimisation, breach notification |

Compliance is a floor, not the goal: a certificate proves a process exists, not that you
cannot be breached. Good programs build real controls and collect compliance evidence from
them as a by-product.

## Metrics and leadership

Report **outcomes** and trends, not raw counts that just measure scanner noise: time to fix
by severity, open KEV exposure on internet-facing systems, coverage (repositories with secret
scanning, services with MFA, assets with owners), time to detect and contain incidents,
results of exercises. Talk about **risk in business terms** (what could happen, how likely,
what it would cost, what it takes to reduce it), present options rather than vetoes, and
remember that security exists to let the business move safely, not to stop it.

## Summary

- Build security into each SDLC stage (SSDF, SAMM); few, trusted, well-placed tools; champions.
- Prioritise vulnerabilities with KEV, EPSS, CVSS and your own context; owners and SLAs.
- Authorised testing and coordinated disclosure complement, not replace, secure development.
- Frameworks (CSF, ISO 27001, SOC 2, PCI DSS, CIS) organise the program; compliance is a floor.
- Outcome metrics and business language make security a partner of delivery.

## Further reading

- NIST SP 800-218, *Secure Software Development Framework*: <https://csrc.nist.gov/pubs/sp/800/218/final>
- OWASP SAMM: <https://owaspsamm.org/>
- FIRST, CVSS v4.0 specification: <https://www.first.org/cvss/v4-0/specification-document>
- FIRST, EPSS: <https://www.first.org/epss/>
- NIST Cybersecurity Framework 2.0: <https://www.nist.gov/cyberframework>
- CIS Critical Security Controls v8: <https://www.cisecurity.org/controls>
- OWASP Vulnerability Disclosure Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Vulnerability_Disclosure_Cheat_Sheet.html>
