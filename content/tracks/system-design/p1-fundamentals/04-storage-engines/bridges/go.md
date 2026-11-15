---
from: go
status: draft
concept: "How data is stored and found"
from_way: "database/sql against PostgreSQL, or an embedded bbolt or BadgerDB file."
native_way: "Know the engine behind each store: bbolt is a B+tree, Badger and Pebble are LSM trees, and the choice shows in write throughput and compaction."
sensei_tip: "Pebble, the LSM engine inside CockroachDB, is written in Go; its documentation explains memtables, SSTables, compaction and Bloom filters with real numbers."
---

| Go | Storage concept |
| --- | --- |
| `go.etcd.io/bbolt` | B+tree engine, update in place |
| `github.com/cockroachdb/pebble` | LSM engine with compaction |
| `github.com/bits-and-blooms/bloom` | Bloom filters |
| `aws-sdk-go-v2/service/s3` | object storage |
| `sort.Search` over sorted keys | how an SSTable index is searched |
