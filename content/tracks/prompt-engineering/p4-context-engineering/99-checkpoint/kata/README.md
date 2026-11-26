# Checkpoint 4: Prompt Optimiser

A ticket-triage prompt gets half its cases wrong. Instead of guessing, you build the loop from
the metaprompting lesson: score the prompt, show an optimiser model a bounded sample of the
failures, check what it proposes, keep it only if it is better, and confirm the result on a
holdout set the optimiser never sees. Write `optimiser.py`.

The dataclasses and constants are given: `Example(id, input, expected)`,
`Failure(id, input, expected, got)`, `Score(rate, failures)`, `Round(number, status, dev_rate, note)`,
`Outcome(prompt, dev_rate, holdout_rate, rounds, shipped, reason)`, `PLACEHOLDER`, `METAPROMPT`
and `FAILURE`. A `Model` takes a list of messages and returns the reply text; a `Grader` takes
`(got, expected)` and returns a bool. The tests use fake models.

## Templates and scores

| Function | Does |
| --- | --- |
| `placeholders(template)` | The names matched by `PLACEHOLDER` (`{{name}}`, lower case, no spaces), sorted, each once. |
| `render(template, values)` | Every placeholder replaced by `html.escape(value, quote=False)`. A placeholder without a value: `ValueError("no value for {{name}}")`. |
| `score(template, examples, model, grader)` | For each example, calls `model` with **one** user message, the template rendered with `{"input": example.input}`, strips the reply and grades it. Returns `Score(passed / total, failures in example order)`. No examples: `ValueError`. |

## The optimiser's side

`render_metaprompt(template, failures, *, max_failures, max_chars)` returns
`METAPROMPT.format(prompt=..., failures=...)`:

- `prompt` is the template escaped with `html.escape(..., quote=False)`;
- `failures` is one `FAILURE.format(...)` block per failure: sorted by id, at most
  `max_failures` of them. The id is escaped with `quote=True`; input, expected and got are each
  **first** cut to `max_chars` characters with `" [truncated]"` added when they were longer, and
  **then** escaped (cutting after escaping could split an entity such as `&lt;`);
- no failures: `ValueError`.

`extract_candidate(reply, required)` takes the **last** `<prompt>...</prompt>` block,
`html.unescape`s and strips it. Errors (`ValueError`):

- no block: `"no <prompt> in the reply"`; an empty one: `"the candidate is empty"`;
- otherwise one problem per placeholder that is required but absent, `"missing {{name}}"`, then
  one per placeholder that is not required, `"unknown placeholder {{name}}"`, each group sorted
  by name and joined with `"; "`.

## The loop

`optimise(seed, *, dev, holdout, model, optimiser, grader, rounds, min_gain=0.0, max_failures=5, max_chars=300)`:

1. Check the setup before any call: the seed's placeholders must be exactly `["input"]`, and no
   example id may be in both sets (`ValueError`).
2. Score the seed on `dev`. It is the current **best**.
3. For each round `1..rounds`: if the best has no failures, stop. Otherwise call `optimiser`
   with a **fresh** list of one user message, the metaprompt for the best and its failures.
   Then log a `Round`:
   - `"invalid"`, `dev_rate=None`, the error message as note, when `extract_candidate(reply, ["input"])`
     fails, or the note `"the candidate is unchanged"` when it equals the best;
   - `"accepted"`, its dev rate, note `""`, when its dev rate is more than `min_gain` above the
     best's: it becomes the best;
   - `"rejected"`, its dev rate, note `"no gain over P"` with the best's rate as `f"{rate:.0%}"`.
4. Score the seed on `holdout`. Then:
   - the best is still the seed: not shipped, the seed with its dev and holdout rates, reason
     `"nothing to improve"` if the seed had no dev failures, else `"no candidate beat the seed"`;
   - the best does **worse** than the seed on holdout: not shipped, the seed with its rates,
     reason `"overfit: holdout fell from A to B"`;
   - otherwise shipped: the best with its rates, reason `"dev A -> B, holdout C -> D"`.

All percentages use `f"{rate:.0%}"`. The holdout examples are only ever sent to `model`, never
to `optimiser`.

Run the visible tests with `uv run pytest`. There are 21 tests: 2 in `tests/test_visible.py`
and 19 hidden ones, plus ruff on `optimiser.py`.
