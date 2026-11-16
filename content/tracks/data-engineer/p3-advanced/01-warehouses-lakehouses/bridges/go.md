---
from: go
status: draft
concept: "Analytical storage"
from_way: "A service writing to PostgreSQL, with reports run against a replica."
native_way: "A warehouse or lakehouse fed by pipelines, with open files on object storage."
sensei_tip: "Many lake building blocks are written in Go (MinIO for S3-compatible storage, parts of Trino's ecosystem tooling); iceberg-go and delta-go let Go services read table metadata."
---

| Go | Warehouses and lakehouses |
| --- | --- |
| a read replica for reports | a stop-gap before a warehouse |
| MinIO | S3-compatible object storage |
| iceberg-go | Iceberg metadata in Go |
| an atomic rename of a pointer file | the idea of a table-format commit |
| cloud.google.com/go/bigquery | a warehouse client |
