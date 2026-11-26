# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): the preflight you run before a new server gets
   its public IP. It reads the SSH configuration the way sshd does, checks that the firewall
   drops by default, lists the sockets that would be reachable from the internet, and finds
   the fstab entries that could leave the server stuck at boot.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Networking: IP, Ports, DNS and SSH | What does `ss -tln` show, and when is a socket reachable only from the machine itself? How do `0.0.0.0`, `[::]` and `127.0.0.1` differ? |
| Disks, Filesystems, Mounts and LVM | Why `UUID=` instead of `/dev/sdb1`? What does `nofail` change at boot? What does a `/dev/mapper/` name point to? |
| Health Report (kata) | How do you read a file such as `/proc/meminfo` or `df -P` with awk and turn the result into an exit status? |
| Hardening a Linux Server | Where do SSH drop-ins live? What does `policy drop` on the input hook do, and why add the SSH rule first? |

## What the kata adds

The hardening lesson tells you to put your SSH settings in a drop-in. The kata shows why
that is not the end of the story: **sshd keeps the first value it reads for each keyword**,
unlike most configuration files, where the last line wins. Debian's main file begins with
`Include /etc/ssh/sshd_config.d/*.conf`, and the drop-ins are read in lexical order, so:

```text
/etc/ssh/sshd_config.d/10-hardening.conf   PasswordAuthentication no    <- this one counts
/etc/ssh/sshd_config.d/50-cloud-init.conf  PasswordAuthentication yes   <- ignored
/etc/ssh/sshd_config                       PasswordAuthentication no    <- ignored
```

Rename `10-hardening.conf` to `99-hardening.conf` and the cloud image's `yes` wins. On a
real server, `sudo sshd -T` prints the settings sshd will really use, and it is the check to
trust. The kata makes you implement the rule so that you remember it.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
