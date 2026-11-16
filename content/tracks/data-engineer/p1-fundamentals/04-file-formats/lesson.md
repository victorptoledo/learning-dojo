# File Formats: CSV, JSON Lines, Avro and Parquet

Pipelines exchange files all day: exports from partners, extracts landed in a lake, tables
stored as files. The format decides how much you can trust what you read, how fast you
can read it, and what happens when the producer adds a field.

## CSV: universal and untyped

CSV is everywhere because every tool writes it. It is also the source of many incidents:

- **No types**: `007` may become `7`, `2026-11-01` a date or a string, `1e5` a number.
- **No single standard**: delimiters (`,` `;` tab), quoting (`"a, b"`), escaped quotes
  (`""`), line breaks inside fields, header or not. RFC 4180 describes the common case.
- **Encodings**: UTF-8 with or without a byte order mark, Windows-1252 from older tools.
- **No nested data**, and no way to say "missing" versus "empty string".

Always use a real parser (Python's `csv`, not `split(",")`), state the encoding, keep the
original file in raw, and cast columns explicitly in staging.

## JSON lines

One JSON object per line (`.jsonl`, also called NDJSON). It is self-describing and handles
nesting, and one bad line does not spoil the others, so it is a common format for API
extracts and logs. Watch for:

- **Numbers**: JSON has one number type. Many parsers read `19.99` as a binary float and
  large integers above 2^53 lose precision in JavaScript. Read decimals as decimals
  (`json.loads(..., parse_float=Decimal)`) and send large ids as strings.
- **Size**: field names repeat on every line; compress the files (gzip, zstd).
- **Schema drift**: nothing stops a producer from sending `"amount": "12"` tomorrow.

## Binary formats with schemas

| | Avro | Parquet |
| --- | --- | --- |
| Layout | **row**-oriented: records one after another | **column**-oriented: each column stored together, in row groups |
| Schema | stored in the file (and in a schema registry for messages) | stored in the file footer |
| Best for | writing record by record: Kafka messages, ingestion | reading a few columns of many rows: analytics |
| Compression | per block | per column chunk, very effective (similar values together) |
| Evolution | rich rules: defaults, aliases, reader and writer schemas | add nullable columns; table formats handle the rest |

**Protobuf** plays the same role as Avro for messages; **ORC** is a columnar sibling of
Parquet from the Hive world. Parquet internals (row groups, statistics, encodings) come in
phase 3.

Binary formats with schemas give you **types** (decimal, timestamp with time zone, lists,
structs) and **splittability**: a big file can be read in parallel chunks. A gzip-compressed
CSV cannot be split; a Parquet file can.

## Schema evolution

Producers change schemas. Decide in advance what is allowed:

| Change | Backward compatible? | How to handle |
| --- | --- | --- |
| add an **optional** field with a default | yes | old readers ignore it, new readers use the default for old data |
| remove an optional field | usually | readers must not require it |
| rename a field | no | add the new one, keep the old (Avro aliases), migrate, then remove |
| change a type (`int` to `string`) | no | a new field, or a new version of the topic or table |
| add a **required** field | no | old data has no value for it |

A **schema registry** (Confluent, Apicurio) stores the versions of each message schema and
rejects incompatible changes before they reach consumers. For files, check the schema of
every incoming file against the expected one, and fail loudly on drift you did not plan.

## Summary

- CSV and JSON are text: universal, but untyped and easy to misread; parse carefully.
- Avro is row-oriented with a schema: good for messages and ingestion.
- Parquet is columnar with a schema: good for analytics, compression and parallel reads.
- Plan schema evolution: optional fields with defaults are safe, renames and type changes
  are not.

## Further reading

- RFC 4180, "Common Format and MIME Type for CSV Files": <https://www.rfc-editor.org/rfc/rfc4180>
- JSON Lines: <https://jsonlines.org/>
- Apache Avro specification, schema resolution: <https://avro.apache.org/docs/current/specification/>
- Apache Parquet documentation, file format: <https://parquet.apache.org/docs/file-format/>
- Confluent documentation, schema evolution and compatibility: <https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html>
- Python documentation, `csv` and `json`: <https://docs.python.org/3/library/csv.html>
