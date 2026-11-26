# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards WAL and
   replication, backups and recovery, and monitoring and tuning. You need 7 right.
2. **The integration kata** (press **Enter**): a disaster-recovery drill on a payments database
   the day after someone ran a clean-up on the wrong server. Find the replication slots that
   hold back WAL, plan the point-in-time restore and check that the archive has every segment
   it needs, put back the rows the accident deleted without undoing an hour of real work, and
   measure the WAL of the nightly import to move its staging table to `UNLOGGED`.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| WAL, Replication and High Availability | What is an LSN, and how many bytes lie between two? Why can a slot fill the disk? What does `wal_status` say? |
| Backups, Point-in-Time Recovery and Upgrades | Which backup can reach a target time? Which WAL segments does a restore replay? What does a hole in the archive mean? |
| Monitoring and Tuning in Production | What do you measure before you change something? |
| Incident Clinic | How do identity columns and sequences behave when rows come back with old ids? |
| Fundamentals and Advanced (checkpoints 1 to 3) | What does `ON DELETE CASCADE` remove without naming it? What does `UNLOGGED` give up? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
