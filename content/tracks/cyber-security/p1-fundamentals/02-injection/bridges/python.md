---
from: python
status: draft
concept: "Keep code and data apart"
from_way: "cursor.execute(f\"... '{name}'\") and os.system with formatted strings."
native_way: "Placeholders with a parameter tuple, allow-listed identifiers, subprocess with a list, and pathlib containment checks."
sensei_tip: "The placeholder style depends on the driver (`?` for sqlite3, `%s` for psycopg); `%s` there is not Python string formatting, so never use the `%` operator yourself."
---

| Python | Injection-safe way |
| --- | --- |
| cursor.execute(f"... {x}") | cursor.execute("... ?", (x,)) |
| "ORDER BY " + sort | SORTS[sort] from an allow-list |
| os.system(cmd) | subprocess.run([...]) without shell |
| os.path.join(base, name) | (base / name).resolve().is_relative_to(base) |
| text(f"...") in SQLAlchemy | text("... :x") with bound params |
