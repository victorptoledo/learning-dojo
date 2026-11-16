---
from: python
status: reviewed
concept: "Plug-ins for the database, and sharing its connections"
from_way: "pip install for a feature, and a SQLAlchemy pool per gunicorn worker."
native_way: "CREATE EXTENSION for features inside the database, small pools, and PgBouncer in front of the server."
sensei_tip: "Behind PgBouncer, use SQLAlchemy's NullPool (let PgBouncer pool) or a tiny QueuePool, and disable psycopg server-side prepared statements (prepare_threshold=None) on older PgBouncer versions."
---

| Python | PostgreSQL |
| --- | --- |
| pip install a library | CREATE EXTENSION |
| create_engine(pool_size=5, max_overflow=10) | up to 15 server connections per worker |
| NullPool behind an external pooler | PgBouncer does the pooling |
| a module-level global | session state, unsafe in transaction mode |
| pgvector's Python package | the pgvector extension's vector type |
