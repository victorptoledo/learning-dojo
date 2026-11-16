# Threat Modeling with STRIDE

Code review finds bugs in what was written. **Threat modeling** finds flaws in what was
*designed*, before anyone writes it, when fixing them costs a whiteboard eraser. A04
*Insecure Design* in the OWASP Top 10 is the category it prevents.

The Threat Modeling Manifesto reduces it to four questions:

1. **What are we working on?** (a model of the system)
2. **What can go wrong?** (threats)
3. **What are we going to do about it?** (mitigations, accepted risks)
4. **Did we do a good enough job?** (review, validate, revisit)

## 1. Model the system: a data flow diagram

A **DFD** has four kinds of elements:

| Element | Drawn as | Examples |
| --- | --- | --- |
| External entity | rectangle | user, partner API, payment provider |
| Process | circle | web app, API, worker |
| Data store | two parallel lines | database, bucket, cache, queue |
| Data flow | arrow | HTTPS request, message, file upload |

Then draw **trust boundaries**: dashed lines where the level of trust changes, such as
internet to DMZ, service to database, tenant A to tenant B, user's browser to your API. Most
threats live **where a data flow crosses a boundary**, so that is where to look hardest.

## 2. Find threats: STRIDE

STRIDE (from Microsoft) gives each threat category the property it violates:

| Threat | Violates | Ask | Typical mitigation |
| --- | --- | --- | --- |
| **S**poofing | authentication | can someone pretend to be another user or service? | strong authentication, mTLS, signed requests |
| **T**ampering | integrity | can data in transit or at rest be changed? | TLS, MACs and signatures, integrity checks, least-privilege writes |
| **R**epudiation | non-repudiation | can someone deny an action and we cannot prove it? | audit logs off the host, signed records |
| **I**nformation disclosure | confidentiality | can data leak to someone who should not see it? | access control, encryption, minimal responses |
| **D**enial of service | availability | can someone exhaust a resource? | rate limits, quotas, timeouts, autoscaling with caps |
| **E**levation of privilege | authorisation | can someone gain rights they should not have? | authorisation checks, sandboxing, least privilege |

Apply it **per element** (STRIDE-per-element: processes can suffer all six, data stores
mostly T, I, D and R, data flows T, I and D) or per interaction across each boundary.
Other aids: **attack trees** (the attacker's goal at the root, ways to reach it as
branches, useful for one high-value asset), **LINDDUN** for privacy threats, and **PASTA**
for a risk-driven, business-centred process. Card games such as Elevation of Privilege make
sessions with developers easier.

## 3. Decide what to do

For every threat: **mitigate** (add a control, and create a ticket), **eliminate** (remove
the feature or the data), **transfer** (a managed service, insurance) or **accept** (with an
owner and a review date). Rate threats so you fix the worst first: a simple likelihood ×
impact matrix is enough; CVSS rates vulnerabilities in existing software, not design threats.

Write the result down where the team works: the DFD, the threat list with decisions, and
links to the tickets. A threat model that lives in a slide deck nobody opens is decoration.

## 4. Make it a habit

- Model **features**, not the whole company: one hour with the people building it, at design
  time, for anything that touches authentication, payments, personal data, new trust
  boundaries or new third parties.
- **Revisit** when the design changes, after an incident, and when a new class of attack
  appears.
- **Validate**: mitigations become tests (abuse cases, security unit tests), and penetration
  tests check the riskiest assumptions.
- Keep models as code where it helps (for example OWASP Threat Dragon or pytm), so they can be
  reviewed and versioned with the system.

## Summary

- Four questions: what are we building, what can go wrong, what will we do, did we do enough.
- DFD with trust boundaries; threats cluster where flows cross boundaries.
- STRIDE: spoofing, tampering, repudiation, information disclosure, denial of service, elevation.
- Decide per threat (mitigate, eliminate, transfer, accept), prioritise, track, revisit.

## Further reading

- Threat Modeling Manifesto: <https://www.threatmodelingmanifesto.org/>
- OWASP Threat Modeling Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Threat_Modeling_Cheat_Sheet.html>
- OWASP Threat Modeling Process: <https://owasp.org/www-community/Threat_Modeling_Process>
- Microsoft, *Threat Modeling Tool threats (STRIDE)*: <https://learn.microsoft.com/en-us/azure/security/develop/threat-modeling-tool-threats>
- OWASP Threat Dragon: <https://owasp.org/www-project-threat-dragon/>
- Adam Shostack, *Threat Modeling: Designing for Security* (Wiley, 2014)
