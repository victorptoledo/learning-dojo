# Checkpoint 4: Final Exam, a Governed Data Product

A shop's warehouse has two sources (`raw.customers`, `raw.orders`), staging models, a
customer dimension, a sales fact and a daily revenue mart. Three of them are **data
products** with owners and freshness objectives. The catalog is in `catalog.py` (given; do
not change it): `Model`, `Column`, `Grant` and the shop itself, `SHOP`.

Build the governance and reliability tooling the platform team needs: lineage down to
columns, personal data that follows the lineage, access with masks and row filters, and the
numbers of an incident (what is stale, who to tell, what to backfill, how much error budget
is left). `governance.py` and `reliability.py` are yours.

A column is named `"model.column"`, and **model names may contain dots**:
`raw.customers.email` is the column `email` of the model `raw.customers`. A `Column` lists
the upstream columns it is computed from (`sources`) and how (`transform`: `copy`,
`derive`, `hash` or `aggregate`). A model without upstream models is a **source**.

## `governance.py`

**`Lineage(models)`** keeps the models in `lineage.models` (a `dict` name -> `Model`;
`models` may be a generator) and refuses a bad catalog with `ValueError`: two models or
two columns of one model with the same name (`duplicate`), an upstream model that does not
exist, or a column source that is not a column of one of the model's **direct** upstream
models (`unknown`), and a cycle, including a model that reads itself (`cycle`).

| Method | Returns |
| --- | --- |
| `order()` | every model after all its upstream models; when several are ready, the alphabetically first goes first |
| `upstream(name)`, `downstream(name)` | every model the model depends on, or that depends on it, directly or not, in `order()`. An unknown model is a `KeyError` |
| `origins(ref)` | the source columns the column is ultimately computed from, sorted (a source column is its own origin). An unknown column is a `KeyError` |
| `impact(ref)` | every column computed from it, directly or not, sorted: what breaks if it changes |

**`personal_data(lineage, tagged)`** takes the source columns tagged as personal data and
returns the set of every personal column: the tagged ones and every column computed from a
personal column by `copy`, `derive` or `hash`. A **hash is still personal data**
(pseudonymised data identifies a person to anyone who can hash a known e-mail). Only an
`aggregate` stops it. Tagging a column of a model that is not a source is a `ValueError`;
an unknown column is a `KeyError`.

**`read(lineage, grants, role, model, rows, personal)`** returns the rows a role may see:

- **Deny by default**: a role missing from `grants`, or a model not in its `Grant.models`,
  is a `PermissionError`.
- A `row_filter` `(column, value)` keeps only rows whose column equals the value. If the
  model has no such column, the filter cannot be applied: return **no rows** (fail closed).
- Unless the grant has `pii=True`, the value of every personal column of the model becomes
  `None`.
- New dicts: the rows given are never changed.

**`erasure_targets(lineage, personal)`** lists, in `order()`, the models that hold at least
one personal column: an erasure request has to rewrite every one of them (and expire their
old snapshots). Models with only aggregates keep their rows.

## `reliability.py`

**`freshness(lineage, loaded_through, now)`** reports `"ok"`, `"stale"` or `"unknown"` for
every product with a `freshness_hours` objective. `loaded_through[model]` is how recent the
data of a model is. A product is **only as fresh as its stalest input**: use the oldest
`loaded_through` of the product and all its upstream models. It is `"ok"` when `now` minus
that is at most `freshness_hours`, and `"unknown"` when the product or any upstream model
has never been loaded.

**`blast_radius(lineage, broken)`** returns `{"models": ..., "products": ..., "owners":
...}`: the broken model and everything downstream of it in `order()`, the products among
them, and the sorted owners of those products, who need to hear about it first.

**`backfill_plan(lineage, changed, start, end)`** lists the `(model, day)` runs that rebuild
the changed model and everything downstream of it, from `start` to `end` inclusive: day by
day, oldest first, and within a day in `order()`. Upstream models are never rerun. `start`
after `end` is a `ValueError`.

**`error_budget(days_met, objective)`** takes one boolean per day (did the product meet its
objective?) and returns:

| Key | Value |
| --- | --- |
| `sli` | the fraction of days met |
| `allowed` | the whole number of days the objective lets you miss, `floor(days × (1 − objective))`, computed **exactly**: 0.9 over 30 days allows 3, not 2 |
| `missed` | the days not met |
| `remaining` | `allowed - missed`, negative when the budget is spent |

No days, or an objective outside `(0, 1]`, is a `ValueError`.

> [!TIP]
> `1 - 0.9` is `0.09999999999999998` in binary floating point. `Fraction` and `Decimal`
> built from the text `"0.9"` are exact.

Run the visible tests with `uv run pytest`. There are 43 tests: 3 in `tests/test_visible.py`
and 37 hidden ones, plus ruff on `catalog.py`, `governance.py` and `reliability.py`.
