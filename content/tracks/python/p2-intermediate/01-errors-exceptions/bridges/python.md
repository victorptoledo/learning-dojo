---
from: python
status: draft
concept: "Modern error handling"
from_way: "Bare except clauses, except Exception: pass, raise inside except without from, error codes returned as None or -1, and lists of error strings collected by hand."
native_way: "Narrow except clauses, raise ... from error, add_note for context, a module exception hierarchy, ExceptionGroup with except*, and contextlib.suppress for deliberate ignores."
sensei_tip: "Grep an old code base for 'except:' and 'except Exception: pass'; each hit is either a hidden bug or a place that needs a comment and contextlib.suppress."
---

| Older habit | Python 3.15 |
| --- | --- |
| `except:` | `except ValueError:` (or `except Exception:` at a boundary) |
| `except Exception: pass` | `with contextlib.suppress(FileNotFoundError):` |
| `raise MyError(str(e))` inside except | `raise MyError("...") from e` |
| `except Exception, e:` (Python 2) | `except Exception as e:` |
| wrapping to add a file name to the message | `e.add_note(f"in {path}")`, then `raise` |
| collecting error strings in a list | `raise ExceptionGroup("invalid", errors)` |
| `return None` meaning "failed" | raise a specific exception |
