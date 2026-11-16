---
from: go
status: draft
concept: "Serialisation formats for data"
from_way: "encoding/json into structs and encoding/csv for flat files."
native_way: "Avro or Protobuf for messages, Parquet for analytical files, all with explicit schemas."
sensei_tip: "json.Decoder.UseNumber() keeps numbers as text until you decide their type, so 19.99 and huge ids survive; encoding/csv already handles quoting and embedded newlines."
---

| Go | Data formats |
| --- | --- |
| encoding/csv | safe CSV parsing |
| json.Decoder.UseNumber() | exact JSON numbers |
| bufio.Scanner per line | JSON lines |
| goavro / protobuf-go | schema-first messages |
| parquet-go | Parquet files |
