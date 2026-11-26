# Checkpoint 5: Prompt Release 🥋

The final exam. The ticket-triage prompt lives in a versioned file, and a new version is about
to go out. Write `release.py`, the small release system around it: it parses the file, checks
that the version number matches what changed, sends a stable share of users to the candidate,
guards every call (retry, fallback, a record of what happened) and decides from those records
whether to promote or roll back.

The dataclasses and constants are given: `PromptVersion(id, version, model, temperature, max_tokens, output, template)`
with the property `label` (`"ticket-triage@2.3.0"`), `Request(model, temperature, max_tokens, messages)`,
`Reply(text, stop_reason)`, `CallRecord(version, attempts, ok, fallback_used, output, problems)`,
and `KEYS`, `SEMVER`, `SNAPSHOT`, `FIELD`, `BUMPS`, `RETRY`. A `Model` takes a `Request` and
returns a `Reply`; the tests use fake ones.

## 1. The prompt file

```text
---
id: ticket-triage
version: 2.3.0
model: claude-sonnet-5-5-20261001
temperature: 0
max_tokens: 400
output: category, priority
---

Classify the support ticket.
<ticket>{{input}}</ticket>
...
```

`parse_prompt_file(text)` returns a `PromptVersion`. The first line must be `---`
(`"missing front matter"`) and another `---` line must close the block
(`"unclosed front matter"`). Inside it, blank lines are skipped and every other line is
`key: value`, split at the first colon and stripped. Errors, with `N` the line number in the file
(the first line is 1), in this order of checks:

| Problem | Message |
| --- | --- |
| a line without a colon | `line N: expected 'key: value'` |
| a key not in `KEYS` | `line N: unknown key 'KEY'` |
| a key seen before | `line N: duplicate key 'KEY'` |
| a key of `KEYS` that is absent (the first one, in `KEYS` order) | `missing key 'KEY'` |
| a version that does not fully match `SEMVER` | `invalid version 'V'` |
| a model that does not fully match `SNAPSHOT` (a moving alias) | `model 'M' is not a pinned snapshot` |
| a temperature that is not a number from 0 to 1 | `invalid temperature 'V'` |
| `max_tokens` that is not a whole number above 0 | `invalid max_tokens 'V'` |
| an output list (comma-separated) with a name that does not match `FIELD`, or a repeated name | `invalid output 'V'` |
| a template (the rest of the file, stripped) without `{{input}}` | `the template has no {{input}}` |

`version` becomes a tuple of three ints, `temperature` a float, `max_tokens` an int and `output`
a tuple of names.

## 2. The release check

`required_bump(old, new)` is the smallest release the change needs: `"major"` when `output`
changed (the parsing code must change too), else `"minor"` when the model, temperature or
max_tokens changed, else `"patch"` when the template changed, else `None`.

`check_release(old, new)` returns a list with at most one problem, checked in this order:

1. a different id: `"id changed from 'OLD' to 'NEW'"`;
2. a version that is not the next major (`X+1.0.0`), minor (`X.Y+1.0`) or patch (`X.Y.Z+1`):
   `"NEW does not follow OLD"`;
3. no change at all: `"nothing changed since OLD"`;
4. a release smaller than `required_bump`: `"this change needs a NEEDED release, not a ACTUAL"`.

A bigger release than needed is allowed. `[]` means the release may go out.

## 3. Rollout

- `bucket(key)` is `int(sha256(key as UTF-8).hexdigest(), 16) % 100`.
- `pick(user_id, stable, candidate, percent)` returns the candidate when
  `bucket(f"{candidate.label}:{user_id}") < percent`, else the stable version. The same user
  always gets the same answer for the same candidate, and a new candidate reshuffles the users.
  A percent outside 0 to 100: `ValueError`.

## 4. Guarded calls

- `build_request(version, input_text)`: the version's model, temperature and max_tokens, and one
  user message, the template with `{{input}}` replaced by `html.escape(input_text, quote=False)`.
- `check_output(version, reply)` returns `(data, [])` when the reply meets the contract, else
  `(None, problems)`: `["truncated"]` when the stop reason is `"max_tokens"`, `["refused"]` when
  it is `"refusal"`, `["not valid JSON"]`, `["not a JSON object"]`, or one
  `"missing key 'K'"` per output name that is absent, in `output` order.
- `call(version, input_text, model, *, fallback=None)` returns a `CallRecord` whose `version` is
  always the primary version's label:
  1. call the model once and check the reply;
  2. on a **format** problem (anything but truncated or refused), retry **once**: the same
     parameters, the first messages plus `{"role": "assistant", "content": reply.text}` and
     `{"role": "user", "content": RETRY.format(problems=...)}` with the problems joined by
     `"; "`. A truncated or refused reply is not retried: the same request would fail again;
  3. if it still fails and there is a fallback version, call it **once** with its own fresh
     request. The record is then `fallback_used=True`, `ok` and `output` come from the fallback,
     and `problems` are the primary's problems followed by the fallback's, each as
     `"fallback: PROBLEM"`;
  4. `attempts` counts every model call.

## 5. The decision

`decide_rollout(records, candidate, *, min_calls, max_failure_rate)` looks only at the records
of the candidate's label. A record is a failure when it is not `ok` **or** used the fallback
(users got the fallback, not the candidate). With fewer than `min_calls` records it returns
`"rollback"` if the failures already exceed `max_failure_rate * min_calls` (the candidate can no
longer pass), else `"wait"`. Otherwise it returns `"rollback"` when the failure rate is above
`max_failure_rate`, else `"promote"`.

Run the visible tests with `uv run pytest`. There are 33 tests: 2 in `tests/test_visible.py`
and 31 hidden ones, plus ruff on `release.py`.
