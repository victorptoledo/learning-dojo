# Incident Response and Forensics Basics

A security **incident** is an event that actually or potentially harms the confidentiality,
integrity or availability of your systems or data: an attacker on a server, a leaked key,
ransomware, a misdirected data export. How well you respond is decided mostly **before** it
happens.

## The life cycle

NIST SP 800-61 Revision 3 (2025) maps incident response onto the six functions of the NIST
Cybersecurity Framework 2.0: **Govern, Identify, Protect** (preparation, all the time) and
**Detect, Respond, Recover** (during and after an incident), with lessons learned feeding
back into all of them. The classic phases from earlier revisions are still the way most
teams work through an incident:

| Phase | What you do |
| --- | --- |
| **Preparation** | an IR plan and playbooks, roles and an on-call rota, contact lists (legal, provider, insurer), logging in place, tooling and access ready, exercises |
| **Detection and analysis** | triage the alert, confirm the incident, determine scope and severity, start the timeline |
| **Containment** | stop the spread: isolate hosts, revoke credentials, block indicators, short-term then long-term |
| **Eradication** | remove the cause: malware, backdoors, compromised accounts, the exploited vulnerability |
| **Recovery** | restore from known-good state, monitor closely, return to normal in steps |
| **Post-incident activity** | a blameless review, root causes, actions with owners, updated detections and playbooks |

## Roles and communication

- **Incident commander**: coordinates and decides; does not do the hands-on work.
- **Scribe**: keeps the timeline (UTC times, what was seen, what was done, by whom).
- **Technical leads** for each area, and a **communications lead** for leadership,
  customers and, through legal, regulators.
- Use a dedicated channel (out of band if the attacker may be in your email or chat).
- **Severity levels** decide who is woken up and how often updates go out.

## Evidence first

Changing a system destroys evidence, so collect what you can before you clean up:

1. **Volatile data first** (order of volatility, RFC 3227): memory, running processes,
   network connections, logged-in users; then disks, then logs and backups.
2. In the cloud: snapshot volumes, capture instance memory if you can, export audit logs
   (CloudTrail, Activity Log), flow logs and the IAM state.
3. Record hashes of collected files and **chain of custody** (who collected what, when,
   where it is stored) in case of legal action.
4. Isolate rather than power off: a security group that blocks traffic keeps memory and the
   attacker's state for analysis; shutting down loses both.

**Indicators of compromise** (IP addresses, domains, file hashes, unusual processes) found in
one place are searched across all systems to find the full scope.

## Containment, eradication, recovery

- Contain fast, but **think before you tip off** an active attacker: if you rotate one key
  while they hold three others, they will notice and dig in. Scope first when you can.
- Revoke and rotate **every** credential the compromised system or identity could reach.
- **Rebuild, do not clean**: redeploy from known-good images and code, restore data from
  backups made before the compromise, patch the vulnerability that let them in.
- Watch closely afterwards: attackers often try to return.

**Ransomware** plans need offline or immutable backups, tested restores, and a decision
process (with legal) prepared before you need it.

## Notification

Many laws set deadlines: under GDPR, a personal data breach must be reported to the
supervisory authority within **72 hours** of becoming aware, and Brazil's LGPD has a similar
duty (the ANPD regulation sets three working days). Sector rules (PCI DSS, health, SEC
disclosure for listed US companies) add others. Involve legal and privacy early; the
timeline you kept is the basis of every notification.

## Learning

A **blameless** post-incident review asks what made the mistake possible, not who made it:
the timeline, what went well, the root and contributing causes, and actions (new detections,
fixed design, updated playbooks) with owners and due dates. Exercises (tabletops,
game days, purple-team tests) keep the plan alive between real incidents.

## Summary

- Preparation decides most outcomes: plan, roles, logs, access, exercises.
- Detect and analyse, contain, eradicate, recover, learn; keep a UTC timeline.
- Preserve evidence before changing systems; isolate instead of powering off.
- Rotate everything reachable, rebuild from known-good, watch for a return.
- Know your notification deadlines; run blameless reviews with tracked actions.

## Further reading

- NIST SP 800-61 Rev. 3, *Incident Response Recommendations and Considerations for Cybersecurity Risk Management*: <https://csrc.nist.gov/pubs/sp/800/61/r3/final>
- NIST SP 800-86, *Guide to Integrating Forensic Techniques into Incident Response*: <https://csrc.nist.gov/pubs/sp/800/86/final>
- RFC 3227, *Guidelines for Evidence Collection and Archiving*: <https://www.rfc-editor.org/rfc/rfc3227>
- CISA, *Federal Government Cybersecurity Incident and Vulnerability Response Playbooks*: <https://www.cisa.gov/resources-tools/resources/federal-government-cybersecurity-incident-and-vulnerability-response-playbooks>
- GDPR Article 33, notification of a personal data breach: <https://gdpr-info.eu/art-33-gdpr/>
