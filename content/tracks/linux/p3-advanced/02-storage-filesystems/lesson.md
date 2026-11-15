# Disks, Filesystems, Mounts and LVM

## Block devices

```text
$ lsblk -f
NAME    FSTYPE  LABEL  UUID                                  MOUNTPOINTS
sda
├─sda1  vfat           7A1C-3F2B                             /boot/efi
├─sda2  ext4           2f6e1c4a-...                          /
└─sda3  swap           91d0b7f2-...                          [SWAP]
nvme0n1
└─nvme0n1p1 xfs  data  c3d4e5f6-...                          /data
```

Disks appear as **block devices** in `/dev`: `sda`, `sdb` (SATA/SCSI and most virtual
disks), `nvme0n1` (NVMe), `vda` (some VMs). Partitions add a number: `sda1`,
`nvme0n1p1`. Partition tables are **GPT** today (MBR on old systems), edited with `fdisk`,
`gdisk` or `parted`.

Device names can change between boots; **UUIDs** (from `blkid` or `lsblk -f`) do not. Use
UUIDs in configuration.

## Filesystems and mounting

A **filesystem** organises a partition into files and directories. On Linux servers:
**ext4** (the default on Debian, robust), **XFS** (default on RHEL, good for large files),
**Btrfs** (snapshots and checksums), plus **tmpfs** (in memory) and **vfat** for the EFI
partition.

```bash
sudo mkfs.ext4 -L data /dev/sdb1          # create a filesystem (ERASES the partition)
sudo mkdir -p /data
sudo mount /dev/sdb1 /data                # attach it to the tree
findmnt /data                             # what is mounted where, with options
sudo umount /data                         # detach ("target is busy": lsof +D /data finds who)
```

To mount at every boot, add a line to **`/etc/fstab`**:

```text
# <device>                                  <mount point>  <type>  <options>          <dump> <fsck>
UUID=c3d4e5f6-0000-4000-8000-123456789abc  /data          ext4    defaults,noatime   0      2
```

Then run `sudo mount -a` (and `systemctl daemon-reload`): it mounts everything in fstab and
shows a mistake **now**, not at the next reboot. A bad fstab line can stop a server from
booting; add `nofail` for disks the system can live without.

Options you will meet: `ro` read-only, `noatime` (do not write access times), `noexec`
(no programs run from here, good for `/tmp` or uploads), `nosuid`, `nodev`.

## What fills the disk

```bash
df -h                       # size, used and free per mounted filesystem
df -i                       # INODES used and free
sudo du -xh --max-depth=1 /var | sort -h      # biggest directories, staying on one filesystem
sudo du -sh /var/log/* | sort -h | tail
ncdu -x /                   # interactive (package ncdu)
```

Three classic surprises:

1. **`df` says full, `du` finds nothing.** A process still has a **deleted** file open
   (a log rotated without telling the program). The space is freed when the process closes
   it. Find it with `sudo lsof +L1`, then restart the program, or empty the file through
   `/proc/PID/fd/N` (`: > /proc/812/fd/7`).
2. **`df -h` shows free space but writes fail with "No space left on device".** Check
   `df -i`: the filesystem ran out of **inodes** (millions of tiny files: sessions, caches,
   mail queues). Only deleting files helps.
3. **About 5% is missing.** ext4 reserves blocks for root so the system can still work when
   users fill the disk (`tune2fs -m 1` lowers it on data disks).

## Swap

**Swap** is disk space the kernel uses for memory pages that have not been used for a
while. It lets the system survive a spike, but a server that swaps constantly is slow:

```bash
swapon --show; free -h
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile   # plus a line in fstab: /swapfile none swap sw 0 0
```

`vm.swappiness` (sysctl) controls how eagerly the kernel swaps. Kubernetes nodes and many
databases prefer no swap at all, so memory limits behave predictably.

## LVM: flexible volumes

The **Logical Volume Manager** puts a layer between disks and filesystems:

```text
disks / partitions  ->  physical volumes (PV)  ->  volume group (VG)  ->  logical volumes (LV)  ->  filesystems
/dev/sdb, /dev/sdc      pvcreate                   vgcreate data          lvcreate -n pg            mkfs.ext4
```

A volume group is a pool of space from one or more disks; logical volumes are cut from it
and can **grow** later, even while mounted:

```bash
sudo pvcreate /dev/sdb && sudo vgcreate data /dev/sdb
sudo lvcreate -n pg -L 50G data && sudo mkfs.ext4 /dev/data/pg
# later, after adding a disk:
sudo pvcreate /dev/sdc && sudo vgextend data /dev/sdc
sudo lvextend -r -L +100G /dev/data/pg     # -r also grows the filesystem (resize2fs / xfs_growfs)
```

`pvs`, `vgs`, `lvs` show the layers. LVM also takes **snapshots** (a frozen view for a
consistent backup). XFS can grow but never shrink; ext4 can shrink only unmounted.

## Summary

- Disks are block devices (`lsblk -f`); refer to filesystems by UUID.
- `mkfs` creates, `mount` attaches, `/etc/fstab` makes it permanent; test with `mount -a`.
- `df -h`, `df -i`, `du -xh --max-depth=1 | sort -h`; deleted open files with `lsof +L1`.
- Swap absorbs spikes; constant swapping means not enough memory.
- LVM: PV, VG, LV; `lvextend -r` grows a volume and its filesystem online.

## Further reading

- Arch Wiki, "fstab": <https://wiki.archlinux.org/title/Fstab> and "LVM": <https://wiki.archlinux.org/title/LVM>
- `mount(8)`: <https://man7.org/linux/man-pages/man8/mount.8.html> and `fstab(5)`: <https://man7.org/linux/man-pages/man5/fstab.5.html>
- The Debian Administrator's Handbook, "RAID and LVM": <https://debian-handbook.info/browse/stable/>
- ext4 documentation: <https://docs.kernel.org/admin-guide/ext4.html>
