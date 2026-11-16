---
from: python
status: reviewed
concept: "Safe migrations in Django and Alembic"
from_way: "RenameField in one migration, applied while old workers are still serving."
native_way: "Split it into additive migrations, backfill in batches with RunPython or a management command, use AddIndexConcurrently, and gate the switch with a flag."
sensei_tip: "django-pg-zero-downtime-migrations or the strong_migrations idea (from Rails) warns you when a migration would lock a big table."
---

| Python | Production ownership |
| --- | --- |
| `RenameField` | add, dual write, backfill, switch, drop |
| `AddIndexConcurrently` | indexes without blocking writes |
| `RunPython` in batches | safe backfills |
| OpenFeature Python SDK | feature flags |
