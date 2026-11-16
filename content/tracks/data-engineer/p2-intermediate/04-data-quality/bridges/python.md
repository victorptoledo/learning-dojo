---
from: python
status: draft
concept: "Testing data, not only code"
from_way: "pytest for functions, and assert statements scattered in notebooks."
native_way: "Declared checks per dataset (dbt tests, Great Expectations, Soda, or your own) that run each batch and gate publication."
sensei_tip: "pandera and Pydantic validate a DataFrame or a record against a schema; combine them with batch checks (uniqueness, volume) and keep pytest for the transformation functions."
---

| Python | Data quality |
| --- | --- |
| assert df.id.is_unique | a unique check |
| pandera DataFrameSchema | schema and validity checks |
| Pydantic model per record | validity at ingestion |
| great_expectations | declared expectations with reports |
| pytest fixtures | unit tests with edge-case inputs |
