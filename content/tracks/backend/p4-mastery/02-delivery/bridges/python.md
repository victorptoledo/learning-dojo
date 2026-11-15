---
from: python
status: draft
concept: "Shipping changes"
from_way: "Deploy scripts or a PaaS, and Django migrations applied in the same step."
native_way: "Small changes, automated pipelines, progressive rollouts and expand-and-contract migrations."
sensei_tip: "A Django migration that renames a column breaks the old code still running during a rolling deploy; split it into add, backfill, switch and drop."
---

| Python | In practice |
| --- | --- |
| manage.py migrate during deploy | expand, migrate, contract |
| RenameField | add new column + backfill + drop old |
| a big release | small, frequent deploys |
| django-waffle | feature flags |
