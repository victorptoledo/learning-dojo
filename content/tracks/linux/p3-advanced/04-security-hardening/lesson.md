# Hardening a Linux Server

A server with a public address is scanned within minutes of booting. Hardening is not one
magic setting; it is removing what attackers can reach, and limiting what they can do if
they get in.

## 1. Accounts and SSH

Log in with your own user and sudo, never as root, and only with keys. Put your changes in a
drop-in, which survives package upgrades:

```text
# /etc/ssh/sshd_config.d/10-hardening.conf
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
PubkeyAuthentication yes
AllowGroups ssh-users
MaxAuthTries 3
X11Forwarding no
```

```bash
sudo groupadd ssh-users && sudo usermod -aG ssh-users ada
sudo sshd -t                   # validate the configuration: no output means OK
sudo systemctl reload ssh      # the unit is ssh.service on Debian (sshd on Fedora/RHEL)
```

> [!WARNING]
> Keep your current SSH session open and test the new configuration from a **second**
> terminal before closing it. A typo here, or a firewall rule in the wrong order, locks you
> out; on a cloud VM your only way back is the provider's serial console.

## 2. A default-deny firewall

Allow only what the server serves. With **nftables**, Debian's native firewall:

```text
# /etc/nftables.conf
table inet filter {
    chain input {
        type filter hook input priority 0; policy drop;
        ct state established,related accept      # replies to connections we made
        iif lo accept                            # local traffic
        ct state invalid drop
        meta l4proto { icmp, ipv6-icmp } accept  # ping and IPv6 neighbour discovery
        tcp dport { 22, 443 } accept             # SSH and HTTPS
    }
    chain forward { type filter hook forward priority 0; policy drop; }
    chain output  { type filter hook output priority 0; policy accept; }
}
```

```bash
sudo nft -c -f /etc/nftables.conf     # check the syntax
sudo systemctl enable --now nftables
sudo nft list ruleset
```

The friendlier `ufw` does the same: `ufw default deny incoming`, `ufw allow OpenSSH`,
`ufw allow 443/tcp`, `ufw enable`. In the cloud, also restrict the **security group**: two
layers are better than one. Databases and admin ports should listen on private addresses
only (`ss -tlnp` from the networking lesson).

## 3. Updates

Most break-ins use known, already-fixed holes. Install security updates automatically:

```bash
sudo apt install unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades    # enables daily security updates
cat /var/log/unattended-upgrades/unattended-upgrades.log
```

Kernel and libc updates need a **reboot** (or at least a restart of the services using the
old library): check `/run/reboot-required`, and `needrestart` lists the services to restart.

## 4. Least privilege

- **Remove what you do not use**: `systemctl list-units --type=service --state=running`
  and `ss -tlnp`; every listening service is attack surface.
- **One user per service**, with no shell and no home it can write (`User=`,
  `DynamicUser=` in systemd).
- **Sandbox** services with systemd (`NoNewPrivileges=`, `ProtectSystem=strict`,
  `PrivateTmp=`, `CapabilityBoundingSet=`), as in the systemd lesson, and check them with
  `systemd-analyze security`.
- **Secrets** at mode `600`, owned by the service user, never in the repository or in a
  unit file readable by everyone; systemd's `LoadCredential=` hands them to the service
  through a private directory.
- **sudo rules** that allow exact commands, not `ALL`.

## 5. Mandatory access control

Permissions are **discretionary**: the owner of a file decides. **Mandatory access
control** adds rules the owner cannot change. Debian and Ubuntu enable **AppArmor**
(profiles per program, `aa-status`); Fedora and RHEL use **SELinux** (labels on every file
and process, `getenforce`, `ls -Z`). When an access is denied although the permissions look
right, check the MAC logs (`journalctl -k | grep -i apparmor`, or `ausearch -m avc` for
SELinux) instead of disabling it.

## 6. Watching

```bash
journalctl -u ssh --since today | grep -E 'Failed|Invalid user'   # brute-force attempts
last -n 20; lastb | head                                          # logins and failed logins
sudo journalctl _COMM=sudo                                        # every sudo use
```

Debian 12 and 13 no longer install rsyslog by default, so there is no `/var/log/auth.log`:
authentication logs are in the journal. **fail2ban** reads those logs and bans addresses
that fail repeatedly. Send logs to a central place: an attacker who gets root can edit the
local ones.

Other checks worth knowing: `find / -xdev -perm -4000` (setuid programs, from the
permissions lesson), `debsums` (changed package files), and **Lynis** (`lynis audit system`)
for a full audit with suggestions.

## Summary

- Personal users, sudo, SSH with keys only, no root login; `sshd -t` and a second session before you reload.
- Default-deny inbound firewall (nftables or ufw), plus the cloud security group.
- `unattended-upgrades` for security fixes; reboot for kernel updates.
- Fewer services, one user per service, systemd sandboxing, secrets at `600`.
- AppArmor or SELinux stay on; read their denials.
- Watch the journal for failed logins and sudo, ship logs off the machine.

## Further reading

- Debian security information and advisories: <https://www.debian.org/security/>
- Arch Wiki, "Security": <https://wiki.archlinux.org/title/Security>
- nftables wiki: <https://wiki.nftables.org/wiki-nftables/index.php/Main_Page>
- Debian Wiki, "UnattendedUpgrades": <https://wiki.debian.org/UnattendedUpgrades>
- `sshd_config(5)`: <https://man7.org/linux/man-pages/man5/sshd_config.5.html>
