# Security Logging, Monitoring and Detection

Prevention fails sometimes. **Detection** is how you find out in minutes instead of months,
and good logs are how you answer *what happened?* afterwards. "Security Logging and Alerting
Failures" is A09 in the OWASP Top 10 because many breaches are discovered by outsiders, long
after they started.

## What to log

Log **security events**, as structured data (JSON), with consistent fields:

| Event | Examples |
| --- | --- |
| Authentication | login success and failure, MFA challenge and result, logout, lockout, password reset requested and completed |
| Account changes | email, password or MFA changed, new API key, role granted |
| Authorisation | access denied (403), admin actions, access to sensitive records |
| Input and abuse | validation failures at unusual rates, rate limit hits, CSRF token failures |
| System | configuration changes, service start and stop, deploys, errors in security controls |

Fields to include: **when** (UTC timestamp), **who** (user or service id), **from where**
(source IP, user agent), **what** (event type, target object), **result** (success or
failure, reason), and a **request or trace id** to join events across services.

**Never log**: passwords (including the wrong ones people type: they are often a real
password for another account), session ids, access and refresh tokens, API keys, full card
numbers, and personal data you do not need. Mask (`card=**** 4242`) or hash when you must
correlate.

## Keep logs trustworthy

- **Log injection**: a username like `ada\nINFO login ok user=admin` forges a log line in
  plain-text logs. Structured logging (JSON fields) or encoding of control characters stops
  it. Parsers must also not trust any field an attacker controls; read fixed parts of a line
  from a position you control (for example from the end).
- **Ship logs off the host** quickly: an attacker with root on a server can edit its local
  files. Central storage with restricted write access, append-only or WORM retention, and
  separate credentials.
- **Synchronise clocks** (NTP) or the timeline of an incident falls apart.
- **Retention**: long enough to investigate (often 90 days hot, a year or more archived;
  regulations such as PCI DSS set minimums), short enough to respect privacy rules.

## From logs to detections

A **SIEM** (Splunk, Elastic Security, Microsoft Sentinel, Wazuh) collects logs and runs
detection rules. Common rule shapes:

| Shape | Example |
| --- | --- |
| **Threshold** | 10 failed logins for one account in 5 minutes |
| **Distinct count** | one IP failing against 20 different accounts in 10 minutes (password spraying) |
| **Sequence** | a success from an IP right after many failures from it (a brute force that worked) |
| **Anomaly** | a login from a new country 30 minutes after one from home (impossible travel) |
| **Indicator match** | traffic to a known-bad domain or hash from threat intelligence |

**MITRE ATT&CK** catalogues attacker techniques (T1110 Brute Force, T1078 Valid Accounts,
T1059 Command and Scripting Interpreter...). Mapping rules to techniques shows what you
detect and where the gaps are. **Sigma** is an open, vendor-neutral format for writing such
rules once and converting them to each SIEM.

On a single Linux host, `journalctl -u ssh`, `/var/log/auth.log` and tools like **fail2ban**
(which bans IPs after repeated failures) apply the same ideas.

## Alerts people act on

An alert nobody reads is worse than none: it creates false confidence. For each rule:

- define **severity** and **who** gets paged; page only for things that need action now;
- write a short **runbook**: what to check, how to contain (block an IP, lock an account,
  revoke tokens), whom to tell;
- measure and tune **false positives**; an allow-list for the office NAT beats muting the rule;
- test the rule: replay a recorded attack or inject a synthetic event and check that it
  fires (detection as code, versioned and reviewed like application code).

## Summary

- Log security events as structured data with who, what, when, where, result and a trace id.
- Never log secrets or tokens; defend against log injection; ship logs off the host.
- Detections: thresholds, distinct counts, sequences, anomalies, indicators; map them to ATT&CK.
- Every alert needs an owner, a runbook and tuning.

## Further reading

- OWASP Logging Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html>
- OWASP Logging Vocabulary Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Logging_Vocabulary_Cheat_Sheet.html>
- NIST SP 800-92, *Guide to Computer Security Log Management*: <https://csrc.nist.gov/pubs/sp/800/92/final>
- MITRE ATT&CK, T1110 Brute Force: <https://attack.mitre.org/techniques/T1110/>
- Sigma rules: <https://sigmahq.io/>
