---
from: python
status: draft
concept: "Modern date and time handling"
from_way: "datetime.utcnow(), pytz with localize() and normalize(), naive datetimes everywhere, dateutil for ISO parsing, and time.time() for durations."
native_way: "datetime.now(UTC), zoneinfo (3.9+) with the fold attribute, fromisoformat that accepts Z (3.11+), and time.monotonic for durations."
sensei_tip: "With zoneinfo you pass tzinfo= directly; the pytz rule \"never pass tzinfo, always localize\" is gone, and so is pytz."
---

| Older habit | Python 3.15 |
| --- | --- |
| `datetime.utcnow()` | `datetime.now(UTC)` |
| `pytz.timezone("Europe/Lisbon").localize(dt)` | `dt.replace(tzinfo=ZoneInfo("Europe/Lisbon"))` |
| `is_dst=` in pytz | `fold=0` / `fold=1` |
| `dateutil.parser.isoparse(s)` | `datetime.fromisoformat(s)` |
| `time.time()` around a job | `time.monotonic()` |
| `datetime.now()` inside the function | an injected `now` callable |
