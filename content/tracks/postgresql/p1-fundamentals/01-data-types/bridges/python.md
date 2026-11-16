---
from: python
status: reviewed
concept: "Types that carry rules"
from_way: "enum.Enum, Pydantic validators, list fields and timezone-aware datetime objects."
native_way: "Enums, domains, arrays and ranges in the database, with exclusion constraints that hold for every client."
sensei_tip: "psycopg maps tstzrange to psycopg.types.range.Range and text[] to a list. Always use aware datetimes (datetime.now(timezone.utc)) with timestamptz."
---

| Python | PostgreSQL |
| --- | --- |
| class Status(enum.StrEnum) | ENUM, or text with CHECK |
| a Pydantic EmailStr field | CREATE DOMAIN email ... |
| list[str] | text[] |
| datetime with tzinfo | timestamptz |
| Range(lower, upper, bounds='[)') | tstzrange |
| an "if not overlapping: insert" check | EXCLUDE USING gist |
