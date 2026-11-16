# Hardening Servers and Services

**Hardening** means removing everything an attacker could use that you do not need, and
tightening what is left. Default installations favour convenience; production systems should
favour a small attack surface. A02 *Security Misconfiguration* is the second category of the
OWASP Top 10 for a reason.

## Start from a baseline

Do not invent your own list. Use a published baseline and record your deviations:

- **CIS Benchmarks** (Debian, Ubuntu, RHEL, Windows, Kubernetes, Docker, cloud accounts),
  each with scored recommendations and audit commands;
- **DISA STIGs** in government contexts;
- vendor hardening guides and NIST SP 800-123 for servers in general.

Tools such as Lynis, OpenSCAP and the CIS-CAT assessor check a host against a baseline.

## Patch, and patch automatically

Most real intrusions use **known** vulnerabilities with available fixes. Keep an inventory of
what runs where, subscribe to advisories, and automate: `unattended-upgrades` on Debian and
Ubuntu installs security updates daily; plan reboots for kernel updates; rebuild container
images regularly so they pick up base image fixes. CISA's Known Exploited Vulnerabilities
catalogue tells you which CVEs attackers use right now: those come first.

## Minimise

Uninstall packages and disable services you do not need (`systemctl list-unit-files
--state=enabled`), close ports (`ss -tlnp` shows what listens), remove compilers and debug
tools from production images, and prefer minimal or distroless images for containers.

## SSH

| Setting (`/etc/ssh/sshd_config`) | Why |
| --- | --- |
| `PasswordAuthentication no`, `KbdInteractiveAuthentication no` | keys only: stops password guessing entirely |
| `PermitRootLogin no` (or `prohibit-password`) | log in as a named user, then `sudo` (accountability) |
| `PermitEmptyPasswords no` | never accept an empty password |
| `MaxAuthTries 3` to `4` | fewer guesses per connection |
| `AllowGroups ssh-users` | only listed accounts may log in |
| `X11Forwarding no`, `AllowAgentForwarding no` (unless needed) | fewer features to abuse |
| modern `Ciphers`, `MACs`, `KexAlgorithms` (or the defaults of a current OpenSSH) | no CBC, RC4, MD5, SHA-1 |

Two rules of the file that trip people up: for most keywords **the first value found wins**
(a later line does not override an earlier one, and files in `sshd_config.d/` are included at
the top on Debian), and settings after a `Match` line apply only to that match. Check the
*effective* configuration with `sshd -T`. Put SSH behind a VPN or bastion where possible, and
ban repeated failures (fail2ban) as a second layer.

## Least privilege for services

- Each service runs as its **own unprivileged user**, owns only its files, and cannot write
  its own code.
- **systemd sandboxing** in the unit: `NoNewPrivileges=yes`, `ProtectSystem=strict`,
  `ProtectHome=yes`, `PrivateTmp=yes`, `ReadWritePaths=/var/lib/app`,
  `CapabilityBoundingSet=`; `systemd-analyze security app.service` scores a unit.
- Containers: non-root user, read-only root filesystem, dropped capabilities, no privileged
  mode, no Docker socket mounted.
- Mandatory access control (AppArmor, SELinux) confines a process even when it runs as root.

## Network

A **default-deny** host firewall (nftables, ufw) that allows only the ports the service
needs; databases and admin interfaces listen on private addresses only; **segmentation** so
the web tier can reach the database but not the HR system; **egress filtering** so a
compromised server cannot freely download tools or send data out.

## Keep it hardened

Hardening by hand decays. Describe the configuration as **code** (Ansible, golden images,
Kubernetes manifests), review it like code, and **audit continuously**: a scheduled job or CI
step that parses the configuration and fails on risky settings, so drift becomes a finding
instead of a surprise. That is the next kata.

## Summary

- Start from a baseline such as a CIS Benchmark; patch automatically, known-exploited first.
- Remove what you do not need; SSH with keys only, no root, few tries, allowed groups.
- Services as unprivileged, sandboxed users; default-deny firewalls and segmentation.
- Configuration as code, audited automatically.

## Further reading

- CIS Benchmarks: <https://www.cisecurity.org/cis-benchmarks>
- NIST SP 800-123, *Guide to General Server Security*: <https://csrc.nist.gov/pubs/sp/800/123/final>
- `sshd_config(5)` manual: <https://man.openbsd.org/sshd_config>
- Mozilla OpenSSH security guidelines: <https://infosec.mozilla.org/guidelines/openssh>
- CISA Known Exploited Vulnerabilities Catalog: <https://www.cisa.gov/known-exploited-vulnerabilities-catalog>
- `systemd.exec(5)`, sandboxing options: <https://www.freedesktop.org/software/systemd/man/latest/systemd.exec.html>
