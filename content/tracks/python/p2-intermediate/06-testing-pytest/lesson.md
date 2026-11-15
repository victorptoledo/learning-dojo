# Testing with pytest: Fixtures, Parametrize and Mocks

Every kata in this track is checked by **pytest**, the de facto standard test runner for
Python. Its design is the opposite of ceremony: tests are plain functions, checks are plain
`assert` statements, and setup is injected by name.

## Tests are functions

```python
# tests/test_money.py
from decimal import Decimal
from money import add_vat

def test_adds_standard_vat():
    assert add_vat(Decimal("100.00")) == Decimal("121.00")
```

pytest collects files named `test_*.py` (or `*_test.py`), functions named `test_*` and
methods of classes named `Test*`. When an `assert` fails, it **rewrites** the expression to
show the values on both sides, with a diff for strings, lists and dicts. You never need
`assertEqual`.

```text
uv run pytest                 # everything
uv run pytest -x -q           # stop at the first failure, short output
uv run pytest -k vat          # tests whose name contains "vat"
uv run pytest --lf            # only the tests that failed last time
uv run pytest tests/test_money.py::test_adds_standard_vat -vv
```

## Checking errors and floats

```python
import pytest

def test_rejects_negative_amounts():
    with pytest.raises(ValueError, match="negative"):
        add_vat(Decimal("-1"))

def test_average():
    assert mean([0.1, 0.2]) == pytest.approx(0.15)
```

`match` is a regular expression searched in the message. `pytest.approx` compares floats
(and lists or dicts of floats) with a tolerance; never compare floats with `==`.

## Parametrize

One test function, many cases, each reported separately:

```python
@pytest.mark.parametrize(
    ("amount", "expected"),
    [
        ("100.00", "121.00"),
        ("0.01", "0.01"),
        ("19.99", "24.19"),
    ],
    ids=["round", "smallest", "rounding"],
)
def test_add_vat(amount, expected):
    assert add_vat(Decimal(amount)) == Decimal(expected)
```

## Fixtures

A **fixture** is a function that provides something a test needs. A test asks for it by
naming it as a parameter:

```python
@pytest.fixture
def catalog() -> Catalog:
    return Catalog([Product("TEA-01", "Green tea", Decimal("3.50"))])

def test_finds_by_sku(catalog):
    assert catalog.find("tea-01").name == "Green tea"
```

Code after a `yield` is the **teardown**, and runs even when the test fails:

```python
@pytest.fixture
def database():
    db = connect(":memory:")
    db.migrate()
    yield db
    db.close()
```

| Feature | Meaning |
| --- | --- |
| `scope="function"` (default) | A fresh value for every test: the safest |
| `scope="module"` / `"session"` | Shared and created once: for expensive things such as a container; never share mutable state between tests |
| `conftest.py` | Fixtures defined here are available to every test in the folder and below, without imports |
| `autouse=True` | Applied to every test in scope without being named |
| Fixtures using fixtures | A fixture can take other fixtures as parameters |

### Built-in fixtures

| Fixture | Gives you |
| --- | --- |
| `tmp_path` | A fresh temporary directory (`pathlib.Path`) per test |
| `monkeypatch` | Set attributes, environment variables and dict items for one test, undone afterwards: `monkeypatch.setenv("TZ", "UTC")` |
| `capsys` | Captured stdout and stderr: `capsys.readouterr().out` |
| `caplog` | Captured log records: `assert "retrying" in caplog.text` |

## Fakes and mocks

Unit tests should not touch the network, the clock or a real payment provider. The cleanest
way is **dependency injection plus a fake**: pass the collaborator in, and give tests a small
in-memory version.

```python
class FakeRates:
    def __init__(self, rates: dict[str, Decimal]) -> None:
        self.rates = rates
    def get(self, currency: str) -> Decimal:
        return self.rates[currency]

def test_converts_with_the_current_rate():
    assert convert(Decimal("10"), "USD", FakeRates({"USD": Decimal("1.1")})) == Decimal("11.0")
```

When you cannot inject, or want to assert on calls, use `unittest.mock`:

```python
from unittest.mock import Mock, patch

rates = Mock()
rates.get.return_value = Decimal("1.1")
rates.get.side_effect = TimeoutError     # or make it raise
...
rates.get.assert_called_once_with("USD")

with patch("shop.checkout.send_email") as send_email:   # patch where it is LOOKED UP
    checkout(order)
send_email.assert_called_once()
```

Patch the name **in the module that uses it** (`shop.checkout.send_email`), not where it is
defined. `Mock(spec=RealClass)` refuses attributes the real class does not have, which stops
tests from passing against an API that does not exist. `AsyncMock` does the same for
coroutines.

> [!TIP]
> If a test needs many patches, the code under test probably does too much. Fakes passed as
> arguments usually lead to simpler code and tests that survive refactoring.

## Marks, coverage and more

```python
@pytest.mark.skipif(sys.platform == "win32", reason="POSIX permissions")
@pytest.mark.xfail(reason="bug 1234", strict=True)
@pytest.mark.slow              # a custom mark: register it in pyproject.toml, run with -m "not slow"
```

**Coverage** shows which lines and branches the tests executed:

```text
uv add --dev pytest-cov
uv run pytest --cov=invoicer --cov-branch --cov-report=term-missing
```

Coverage tells you what is *not* tested; a high number does not mean the tests check the right
things.

Two more tools worth knowing: **Hypothesis** generates hundreds of inputs from a description
(`@given(st.decimals(min_value=0))`) and shrinks a failure to the smallest example, which finds
edge cases you would not think of; and **doctest** runs the examples in docstrings. For
`async def` tests, use a plugin such as `pytest-asyncio` or `anyio`'s pytest plugin.

## Summary

- Tests are `test_*` functions with plain `assert`; pytest shows the values on failure.
- `pytest.raises(match=...)`, `pytest.approx`, `@pytest.mark.parametrize`.
- Fixtures inject setup by name, with scopes and `yield` teardown; share them in
  `conftest.py`; use `tmp_path`, `monkeypatch`, `capsys`, `caplog`.
- Prefer injected fakes; use `Mock(spec=...)` and `patch` where the name is looked up.
- Measure coverage with `pytest-cov`; consider Hypothesis for property-based tests.

## Further reading

- pytest documentation, "Get Started": <https://docs.pytest.org/en/stable/getting-started.html>
- pytest, "How to use fixtures": <https://docs.pytest.org/en/stable/how-to/fixtures.html>
- pytest, "How to parametrize fixtures and test functions": <https://docs.pytest.org/en/stable/how-to/parametrize.html>
- `unittest.mock`, including "Where to patch": <https://docs.python.org/3/library/unittest.mock.html#where-to-patch>
- Hypothesis documentation: <https://hypothesis.readthedocs.io/en/latest/>
- roadmap.sh, Python topic "pytest": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/pytest@3FDwJpesfelEyJrNWtm0V.md>
