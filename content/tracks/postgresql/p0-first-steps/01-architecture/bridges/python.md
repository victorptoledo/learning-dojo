---
from: python
status: reviewed
concept: "What sits behind a database connection"
from_way: "psycopg.connect() or a SQLAlchemy engine with a pool, and inspect(engine) to read the schema."
native_way: "Each connection is a server process; pg_catalog holds the metadata that SQLAlchemy's inspector reads."
sensei_tip: "With gunicorn, every worker has its own pool: workers × (pool_size + max_overflow) is your real connection count. Check it in pg_stat_activity."
---

| Python | PostgreSQL |
| --- | --- |
| psycopg.connect("dbname=shop") | a backend process for one database |
| sqlalchemy.inspect(engine).get_table_names() | SELECT relname FROM pg_class WHERE relkind = 'r' |
| conn.info.backend_pid | pg_backend_pid() |
| create_engine(pool_size=5) per worker | 5 processes per worker on the server |
| os.getpid() of a worker | not the pid in pg_stat_activity: that is the server's process |
