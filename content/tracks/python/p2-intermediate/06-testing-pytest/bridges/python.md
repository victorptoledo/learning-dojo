---
from: python
status: draft
concept: "From unittest classes to pytest"
from_way: "unittest.TestCase subclasses, self.assertEqual and self.assertRaises, setUp and tearDown, the mock backport package, nose, and loops inside one test to try several inputs."
native_way: "pytest functions with plain assert, fixtures with yield, parametrize for each input, unittest.mock from the standard library, and tmp_path, monkeypatch and caplog."
sensei_tip: "pytest runs existing unittest.TestCase tests as they are, so migrate gradually: write new tests in pytest style and convert old ones when you touch them."
---

| Older habit | Python 3.15 with pytest |
| --- | --- |
| `class TestX(unittest.TestCase)` | plain `def test_...():` functions |
| `self.assertEqual(a, b)` | `assert a == b` |
| `self.assertRaises(ValueError, f, x)` | `with pytest.raises(ValueError): f(x)` |
| `setUp` / `tearDown` | fixtures with `yield` |
| a `for` loop over cases inside one test | `@pytest.mark.parametrize` |
| `tempfile.mkdtemp()` and manual cleanup | `tmp_path` |
| `import mock` (the old backport) | `from unittest import mock` |
| `nosetests` | `pytest` |
