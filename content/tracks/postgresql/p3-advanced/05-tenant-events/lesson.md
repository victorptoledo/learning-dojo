# Tenant Events: Partitions and Row-Level Security

The two features of this phase that big multi-tenant systems combine: monthly partitions so old
data leaves with a `DROP TABLE` and queries read only the month they need, and row-level
security so the database itself, not every query of the API, keeps one customer's rows away
from another. Press **Enter** to create the kata folder and open it.
