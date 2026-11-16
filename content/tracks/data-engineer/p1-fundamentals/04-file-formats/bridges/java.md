---
from: java
status: draft
concept: "Serialisation formats for data"
from_way: "Jackson for JSON and Java serialisation between services."
native_way: "Avro or Protobuf with a schema registry for messages, Parquet for analytical files."
sensei_tip: "Avro's Java code generation gives you typed records from .avsc files, like Protobuf messages; Jackson's DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS keeps JSON amounts exact."
---

| Java | Data formats |
| --- | --- |
| Jackson ObjectMapper | JSON lines |
| USE_BIG_DECIMAL_FOR_FLOATS | exact decimals from JSON |
| Avro SpecificRecord | typed Avro messages |
| Protobuf generated classes | schema-first messages |
| parquet-java / Spark | reading and writing Parquet |
