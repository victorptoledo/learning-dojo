---
from: csharp
status: draft
concept: "Serialisation formats for data"
from_way: "System.Text.Json and CsvHelper with classes mapped from text."
native_way: "Schema-first binary formats: Avro or Protobuf for messages, Parquet for tables."
sensei_tip: "System.Text.Json reads numbers into decimal when the property is decimal, which keeps cents exact; CsvHelper needs an explicit CultureInfo, or '1,5' becomes 15 on some machines."
---

| C# | Data formats |
| --- | --- |
| CsvHelper with CultureInfo.InvariantCulture | safe CSV |
| System.Text.Json with decimal properties | exact JSON amounts |
| Google.Protobuf | schema-first messages |
| Apache.Avro | Avro records |
| Parquet.Net | Parquet files |
