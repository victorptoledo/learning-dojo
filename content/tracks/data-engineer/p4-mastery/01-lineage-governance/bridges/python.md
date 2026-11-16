---
from: python
status: draft
concept: "Knowing where data comes from and who may see it"
from_way: "Scripts that read from and write to tables nobody documented."
native_way: "Lineage from dbt and OpenLineage, PII tags in the catalog, and governed access per role."
sensei_tip: "openlineage-python and the Airflow provider emit lineage events; in dbt, meta: {pii: true} on a column plus a CI check gives classification where the code lives."
---

| Python | Lineage and governance |
| --- | --- |
| ref('stg_orders') in dbt | lineage built from code |
| openlineage-python client | emitting lineage events |
| hashlib.sha256(email + salt) | pseudonymisation |
| dbt column meta tags | classification in code |
| cryptography.Fernet per person | crypto-shredding keys |
