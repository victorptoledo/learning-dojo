# Boot, the Kernel, cgroups and Namespaces

## From power-on to login

1. **Firmware** (UEFI on modern machines, BIOS on old ones) tests the hardware and starts
   a boot loader from the EFI system partition.
2. The **boot loader**, usually **GRUB**, shows the menu, loads the **kernel**
   (`/boot/vmlinuz-...`) and the **initramfs**, and passes the kernel **command line**.
3. The **kernel** initialises the hardware and starts the initramfs: a tiny temporary root
   filesystem with the drivers and tools needed to find and mount the real root (LVM, RAID,
   encryption).
4. The real root is mounted and the kernel runs **PID 1**: **systemd**.
5. systemd starts units in parallel, following their dependencies, until it reaches the
   **default target**: `multi-user.target` on servers, `graphical.target` on desktops.

```bash
systemd-analyze                     # how long the boot took, firmware to userspace
systemd-analyze blame | head        # slowest units
systemd-analyze critical-chain      # the chain that decided the total time
systemctl get-default
journalctl -b -1 -p err             # errors of the PREVIOUS boot (after a crash)
cat /proc/cmdline                   # the kernel command line of this boot
```

When a server does not boot, the usual causes are a broken `/etc/fstab` line (systemd
drops to an emergency shell), a full `/boot`, or a missing driver in the initramfs. On a
cloud VM, the provider's serial console shows these messages.

## The kernel

```bash
uname -r                            # 6.12.x on Debian 13
lsmod | head                        # loaded modules (drivers)
modinfo ext4 | head -n 5
sudo modprobe br_netfilter          # load a module (and its dependencies)
echo br_netfilter | sudo tee /etc/modules-load.d/k8s.conf    # load it at every boot
```

Most drivers are **modules** loaded on demand. Kernel parameters are files under
`/proc/sys`, read and written with **sysctl**:

```bash
sysctl vm.swappiness
sudo sysctl -w net.ipv4.ip_forward=1                         # until the next reboot
echo 'net.ipv4.ip_forward = 1' | sudo tee /etc/sysctl.d/90-forward.conf   # permanent
sudo sysctl --system                                         # reload every file
```

Boot-time options (such as `systemd.unit=rescue.target` for a rescue boot) go on the
kernel command line, set in `/etc/default/grub` and applied with `sudo update-grub`.
Kernel upgrades arrive as normal packages (`linux-image-amd64`) and need a reboot.

## Namespaces: what a process can see

A **namespace** gives a group of processes its own view of one kind of resource:

| Namespace | Isolates |
| --- | --- |
| `pid` | process IDs: the first process inside is PID 1 and sees only its descendants |
| `net` | network interfaces, addresses, routes, ports, firewall rules |
| `mnt` | the mount table: a different root filesystem |
| `uts` | the hostname |
| `ipc` | shared memory and message queues |
| `user` | user IDs: root inside can map to an unprivileged user outside |
| `cgroup` | the view of the cgroup tree |

```bash
lsns                                 # the namespaces on this machine
sudo unshare --pid --fork --mount-proc bash   # a shell in a new PID namespace: ps shows 2 processes
ls -l /proc/$$/ns                    # the namespaces of your shell
```

## cgroups: how much it can use

**Control groups** (cgroup v2, the only version on Debian 13) organise processes in a tree
under `/sys/fs/cgroup` and limit and account their resources: memory, CPU, I/O, number of
processes. systemd puts every service in its own cgroup, which is how `MemoryMax=` and
`CPUQuota=` work:

```bash
systemd-cgls                          # the tree of cgroups and their processes
systemd-cgtop                         # live usage per cgroup
cat /sys/fs/cgroup/system.slice/nginx.service/memory.current
cat /sys/fs/cgroup/system.slice/nginx.service/memory.max
```

When a cgroup reaches `memory.max`, the kernel reclaims memory inside it and, if that is not
enough, the OOM killer kills a process **of that cgroup**, even when the host has plenty
left.

## So what is a container?

A container is **an ordinary Linux process** started with its own namespaces, placed in a
cgroup with limits, with a root filesystem made from the image's layers (stacked with
**overlayfs**), and usually with fewer capabilities and a seccomp filter. There is no
second kernel: every container shares the host's.

```bash
docker run -d --name web --memory 512m --cpus 1 nginx
ps -ef | grep nginx                   # the host sees it as normal processes
docker inspect -f '{{.State.Pid}}' web
sudo ls -l /proc/<pid>/ns             # its namespaces differ from the host's
```

Consequences you can now explain:

- Containers start in milliseconds and cost little: they are processes, not VMs.
- `--memory` is a cgroup limit; exceeding it is an OOM kill with exit status 137.
- Tools inside a container that read `/proc/meminfo` may see the host's memory; modern JVMs
  read the cgroup limit instead (container support is on by default).
- A kernel vulnerability, a `--privileged` container or a mounted Docker socket can reach
  the host. Virtual machines (or gVisor and Kata Containers) isolate more strongly.

## Summary

- Firmware, GRUB, kernel and initramfs, then systemd (PID 1) up to the default target.
- `systemd-analyze`, `journalctl -b -1`, `/proc/cmdline` explain boots and failed boots.
- Modules with `lsmod` and `modprobe`; parameters with `sysctl` and `/etc/sysctl.d/`.
- Namespaces decide what a process sees; cgroups decide how much it may use.
- A container is a host process with namespaces, a cgroup and an image root filesystem.

## Further reading

- Arch Wiki, "Arch boot process" (applies to every distribution): <https://wiki.archlinux.org/title/Arch_boot_process>
- `namespaces(7)`: <https://man7.org/linux/man-pages/man7/namespaces.7.html> and `cgroups(7)`: <https://man7.org/linux/man-pages/man7/cgroups.7.html>
- Kernel documentation, "Control Group v2": <https://docs.kernel.org/admin-guide/cgroup-v2.html>
- `bootup(7)` from systemd: <https://www.freedesktop.org/software/systemd/man/latest/bootup.html>
