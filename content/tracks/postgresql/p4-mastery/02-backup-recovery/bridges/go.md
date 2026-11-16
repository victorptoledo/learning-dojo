---
from: go
status: reviewed
concept: "Going back in time after a mistake"
from_way: "Writing data to object storage with versioning turned on, and restoring an old version of a file."
native_way: "Base backups plus archived WAL in object storage, replayed to any moment with point-in-time recovery."
sensei_tip: "WAL-G is written in Go and is a common choice on Kubernetes: base backups and WAL to S3, GCS or Azure, with restore commands you can script and test in CI."
---

| Go | PostgreSQL |
| --- | --- |
| S3 object versioning | archived WAL plus base backups |
| restoring an old object version | PITR to a target time |
| a CI job that restores and verifies | an automated restore test |
| go install of a new Go version, tested first | pg_upgrade rehearsed on a copy |
