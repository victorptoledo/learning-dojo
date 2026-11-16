---
from: python
status: draft
concept: "Serialisation formats for data"
from_way: "pandas.read_csv with type inference, and json.loads with floats."
native_way: "Explicit parsing (csv, Decimal), Avro via fastavro, and Parquet via pyarrow."
sensei_tip: "Pass dtype= and keep_default_na=False to read_csv when codes must stay text, and prefer pyarrow tables or Parquet over CSV for anything you read twice."
---

| Python | Data formats |
| --- | --- |
| csv.DictReader | safe CSV parsing |
| json.loads(..., parse_float=Decimal) | exact JSON amounts |
| pd.read_csv(dtype=str) | no type guessing |
| fastavro | Avro records |
| pyarrow.parquet | Parquet files |
