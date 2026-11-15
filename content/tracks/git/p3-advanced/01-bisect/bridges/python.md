---
from: python
status: draft
concept: "Automated regression hunting"
from_way: "Adding print statements to the newest version until the cause appears."
native_way: "git bisect run pytest on one test file kept outside the repository: pytest's exit code already means good (0) or bad (1)."
sensei_tip: "pytest exits 5 when no tests were collected (for example when the module did not exist yet). Wrap it: pytest -q /tmp/test_totals.py; code=$?; [ $code -eq 5 ] && exit 125; exit $code."
---

| Python habit | With bisect |
| --- | --- |
| `print` debugging on main | `git bisect run` over the whole range |
| `pytest tests/test_totals.py` | the run command (copy the test outside first) |
| a commit where imports fail | `exit 125` |
| dependencies changed in the range | `pip install -e .` inside the script |
