---
from: python
status: draft
concept: "Naming and representation conventions"
from_way: "snake_case dicts in one service and camelCase in another; `float` for prices; naive `datetime`s."
native_way: "Pydantic models with `alias_generator=to_camel` (or snake_case everywhere), `Decimal` for money, timezone-aware `datetime` in UTC."
sensei_tip: "A naive `datetime` has no zone and serialises without an offset; always use `datetime.now(timezone.utc)`."
---

| Python | Convention |
| --- | --- |
| `datetime(..., tzinfo=timezone.utc)` | RFC 3339 with Z |
| `Decimal` or `int` cents | money |
| `StrEnum` | named enum values |
| `ConfigDict(alias_generator=to_camel)` | consistent field case |
| `{"items": [...]}` | collection envelope |
