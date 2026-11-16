---
from: javascript
status: draft
concept: "Database internals seen from JavaScript"
from_way: "Firing many small queries in parallel from Node and assuming they are independent."
native_way: "Each query in a pool runs in its own transaction unless you hold one client for a BEGIN … COMMIT block; IndexedDB in browsers is a transactional store built on LevelDB or SQLite."
sensei_tip: "Use one client for a transaction (`const c = await pool.connect()`), and keep transactions short: an open transaction holds back VACUUM and locks."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `pool.query(sql)` | an autocommit transaction |
| `BEGIN` … `COMMIT` on one client | a real transaction |
| IndexedDB | transactional key-value storage in the browser |
| `better-sqlite3` | SQLite B-trees in process |
| an idle open transaction | blocks cleanup of old MVCC versions |
