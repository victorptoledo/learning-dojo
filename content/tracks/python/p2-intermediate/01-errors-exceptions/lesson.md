# Exceptions, Exception Groups and Error Design

Python reports errors with **exceptions**, everywhere: the standard library, your code and
the interpreter itself. Handling them well means knowing the syntax, but mostly knowing
**what to catch, where, and what to raise instead**.

## The hierarchy

```text
BaseException
 ├── KeyboardInterrupt, SystemExit, GeneratorExit   <- not errors: do not catch by accident
 └── Exception                                      <- everything you normally handle
      ├── ValueError        (right type, bad value: int("abc"))
      ├── TypeError         (wrong type: len(5))
      ├── LookupError
      │    ├── KeyError     (d["missing"])
      │    └── IndexError   (xs[99])
      ├── OSError           (FileNotFoundError, PermissionError, TimeoutError, ConnectionError, ...)
      ├── ArithmeticError   (ZeroDivisionError, decimal.InvalidOperation, ...)
      ├── AttributeError, NotImplementedError, RuntimeError, StopIteration, ...
      └── ExceptionGroup    (several at once, 3.11+)
```

`except Exception` never catches `KeyboardInterrupt` or `SystemExit`, which is why a bare
`except:` (which catches everything) is a bug waiting to happen.

## try, except, else, finally

```python
try:
    config = load_config(path)
except FileNotFoundError:
    config = Config.defaults()          # an expected, recoverable case
except (PermissionError, IsADirectoryError) as error:
    raise ConfigError(f"cannot read {path}") from error
else:
    log.info("loaded %s", path)         # runs only if no exception: keeps the try small
finally:
    metrics.increment("config.loads")   # runs always, even on return or raise
```

Catch **the narrowest exception** you can handle, around **the smallest block** that can
raise it. A broad `except Exception` hides bugs; it belongs only at a boundary (see below).

### EAFP

"Easier to ask forgiveness than permission" is the Python style: try the operation and handle
the failure, instead of checking first (LBYL, "look before you leap"):

```python
# LBYL: two lookups, and racy for files (it may vanish between the check and the open)
if os.path.exists(path):
    with open(path) as file: ...

# EAFP: one operation, no race
try:
    with open(path) as file: ...
except FileNotFoundError:
    ...
```

## Raising and chaining

```python
raise ValueError(f"quantity must be positive, got {qty}")

try:
    data = json.loads(body)
except json.JSONDecodeError as error:
    raise BadRequest("body is not JSON") from error    # explicit cause
```

`raise ... from error` sets `__cause__`, and the traceback shows both: "The above exception
was the direct cause of the following exception". Without `from`, Python still records the
first one as `__context__` ("During handling of the above exception, another exception
occurred"), which reads like a bug in your handler. `from None` hides the original when it
would only add noise. A bare `raise` inside `except` re-raises the current exception
unchanged.

> [!TIP]
> ruff's rule B904 (enabled in the dojo's checks) flags a `raise` inside `except` without
> `from`. Chaining costs nothing and saves hours of debugging.

## Your own exceptions

A module or library defines **one base class** and specific subclasses, so callers can catch
all of its errors or just one kind:

```python
class PaymentError(Exception):
    """Base class for errors from the payments module."""

class CardDeclined(PaymentError):
    def __init__(self, reason: str, *, code: str) -> None:
        super().__init__(f"card declined: {reason}")
        self.code = code

class ProcessorUnavailable(PaymentError):
    """Transient: the caller may retry."""
```

Name them with an `Error` suffix (or a clear event name such as `CardDeclined`), give them
attributes callers need, and inherit from the closest built-in when one fits
(`class InvalidAmount(PaymentError, ValueError)`).

### Notes

`add_note()` (3.11) attaches context to an exception on its way up, without wrapping it:

```python
for number, line in enumerate(lines, start=1):
    try:
        rows.append(parse(line))
    except ValueError as error:
        error.add_note(f"in {path}, line {number}")
        raise
```

The note is printed under the exception's message in the traceback.

## Exception groups and except*

Concurrent code can fail in several places at once. An `ExceptionGroup` carries all of the
exceptions, and `except*` handles each kind inside it (3.11+):

```python
def validate(order) -> None:
    problems = []
    if order.qty < 1:
        problems.append(ValueError("qty must be at least 1"))
    if order.currency not in {"EUR", "USD"}:
        problems.append(LookupError(f"unknown currency {order.currency}"))
    if problems:
        raise ExceptionGroup("invalid order", problems)

try:
    validate(order)
except* ValueError as group:        # group.exceptions holds every ValueError
    ...
except* LookupError as group:
    ...
```

You meet groups mostly through `asyncio.TaskGroup` (advanced phase), which raises one when
several tasks fail.

## Where to catch

| Layer | What it does with exceptions |
| --- | --- |
| Low-level code | Raises specific exceptions; translates library errors into the module's own, with `from` |
| Business logic | Usually lets them propagate; catches only what it can really handle (a fallback, a retry) |
| The boundary (HTTP handler, CLI `main`, job runner) | Catches broadly, logs with the traceback (`log.exception(...)`), and turns errors into a response or exit code |

```python
def main() -> int:
    try:
        run()
    except UsageError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2
    except Exception:
        log.exception("unexpected failure")   # logs the full traceback
        return 1
    return 0
```

Never swallow an exception silently (`except Exception: pass`). If ignoring one is really
right, say so in code: `with contextlib.suppress(FileNotFoundError): os.remove(tmp)`.

## Summary

- Catch subclasses of `Exception`, narrowly, around small blocks; never a bare `except:`.
- `else` for the success path, `finally` for cleanup; EAFP over LBYL.
- `raise New(...) from error` keeps the cause; bare `raise` re-raises; `add_note` adds context.
- One base exception per module, specific subclasses with useful attributes.
- `ExceptionGroup` and `except*` for several failures at once.
- Handle broadly only at boundaries, and log with the traceback.

## Further reading

- The Python Tutorial, "Errors and Exceptions": <https://docs.python.org/3/tutorial/errors.html>
- Built-in exceptions and their hierarchy: <https://docs.python.org/3/library/exceptions.html>
- PEP 654, exception groups and except*: <https://peps.python.org/pep-0654/>
- PEP 678, enriching exceptions with notes: <https://peps.python.org/pep-0678/>
- PEP 3134, exception chaining: <https://peps.python.org/pep-3134/>
- roadmap.sh, Python topic "Exceptions": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/exceptions@fNTb9y3zs1HPYclAmu_Wv.md>
