# Injection: SQL, Commands and Paths

Every injection has the same shape: the program builds **code** (a SQL statement, a shell
command, a file path, an HTML page) by pasting in **data** from the user, and the data
contains syntax that the interpreter obeys. The fix always has the same shape too: keep code
and data apart, so the data can never be read as code.

## SQL injection

```python
# Vulnerable
cursor.execute(f"SELECT * FROM users WHERE name = '{name}'")
```

With `name = "' OR '1'='1"` the statement becomes
`SELECT * FROM users WHERE name = '' OR '1'='1'`, which returns every user. With
`' UNION SELECT card_number, cvv FROM payments --` it returns another table. Variants:

- **Error-based**: the database error message leaks data or the schema.
- **Blind**: the page shows nothing, but a yes/no difference (`AND 1=1` vs `AND 1=2`) or a
  delay (`pg_sleep(5)`) lets the attacker read data one bit at a time. Hiding errors does not
  fix injection.
- **Second-order**: the payload is stored safely, then a later query (a report, an admin
  page) pastes it into SQL unsafely.

### The fix: bound parameters

```python
cursor.execute("SELECT * FROM users WHERE name = ?", (name,))       # sqlite3
cursor.execute("SELECT * FROM users WHERE name = %s", (name,))      # psycopg
```

The statement and the values travel **separately**; the database parses the statement first
and only then plugs in the values as data. No quoting or escaping by hand: escaping functions
have been bypassed many times (character sets, forgotten fields), while parameters cannot be.

### What cannot be a parameter

Placeholders stand for **values** only. Table and column names, `ASC`/`DESC`, and `LIMIT` in
some databases cannot be bound. Use an **allow-list** that maps user input to fixed SQL:

```python
SORTS = {"name": "name", "price": "price", "newest": "created_at DESC"}
order = SORTS.get(sort)
if order is None:
    raise ValueError("unknown sort")
cursor.execute(f"SELECT * FROM products ORDER BY {order}")   # only our own strings reach SQL
```

Two more traps:

- **`IN (...)` lists**: build one placeholder per value, `", ".join("?" * len(ids))`, and pass
  the values; never join the values themselves.
- **`LIKE` wildcards**: a bound `%` still means "anything" inside `LIKE`. Escape `%`, `_` and
  the escape character in the value, and add `ESCAPE '\'`, if the user's text must match
  literally.

ORMs parameterise for you, until you use their raw-SQL escape hatches (`.raw()`, `text()`,
`createNativeQuery`, string-built JPQL). Review those like hand-written SQL.

> [!TIP]
> Defence in depth: the application's database account should not own the schema. If an
> injection slips through, `DROP TABLE` and reading other schemas should fail.

## OS command injection

```python
os.system(f"ping -c 1 {host}")                 # host = "8.8.8.8; cat /etc/shadow"
subprocess.run(["ping", "-c", "1", host])      # one argument, no shell: ';' is just a character
```

Pass an **argument list** and no shell (`shell=False`, the default in `subprocess`). Even
then, an argument that starts with `-` can be read as an option (`--output=/etc/...`):
validate the value (an IP address parser, an allow-list) and put `--` before positional
arguments where the program supports it. Best of all, use a library call instead of a
program (`socket`, `shutil`, `zipfile`).

## Path traversal

```python
open(os.path.join("/srv/uploads", filename))   # filename = "../../etc/passwd"
```

`os.path.join` also discards the base when the second part is absolute. Resolve the final
path and check that it is still inside the base directory:

```python
base = Path("/srv/uploads").resolve()
target = (base / filename).resolve()
if not target.is_relative_to(base):
    raise PermissionError("outside the upload directory")
```

Better still, never use the user's name for the file: store uploads under a generated id
and keep the original name only as metadata.

## The same idea elsewhere

LDAP filters, XPath, NoSQL queries (`{"$ne": null}` in a JSON body), template engines
(server-side template injection), log lines (newline injection) and HTML (XSS, the next
lesson) all follow the pattern: use the API that separates structure from data, and
validate input against what you expect.

## Summary

- Injection = data interpreted as code. Keep them apart.
- SQL: bound parameters for values, allow-lists for identifiers and sort orders, one
  placeholder per `IN` value, escape `LIKE` wildcards.
- Commands: argument lists without a shell, validated values, library calls when possible.
- Paths: resolve and check containment, or use generated names.

## Further reading

- OWASP SQL Injection Prevention Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html>
- OWASP Query Parameterization Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Query_Parameterization_Cheat_Sheet.html>
- OWASP OS Command Injection Defense Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/OS_Command_Injection_Defense_Cheat_Sheet.html>
- CWE-89 (SQL injection), CWE-78 (OS command injection), CWE-22 (path traversal): <https://cwe.mitre.org/>
- Python `sqlite3` documentation, "How to use placeholders to bind values": <https://docs.python.org/3/library/sqlite3.html#sqlite3-placeholders>
