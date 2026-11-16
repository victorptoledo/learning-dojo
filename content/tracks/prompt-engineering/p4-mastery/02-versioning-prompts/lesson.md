# Versioning Prompts

A prompt in production is code: it decides what your product does. It deserves what code gets:
a home in version control, review, tests, versions, release notes and a way back. Teams that
skip this find out the hard way, when a well-meant one-word change or a silent model update
breaks a feature and nobody can say what changed.

## Prompts as files

Keep each prompt in its own file (or entry in a prompt registry), with a stable **id**:

```text
prompts/
  ticket-triage/
    prompt.yaml        # metadata: id, version, model, parameters, tools, schema
    system.md          # the system prompt template
    examples.jsonl     # few-shot examples
    evals.jsonl        # the evaluation set
    CHANGELOG.md
```

Separate files make prompt changes **reviewable**: the diff of `system.md` reads like prose, the
diff of `examples.jsonl` shows which examples changed, and the evaluation set sits next to the
prompt it tests.

## What a version contains

The behaviour of a call depends on more than the template, so a **prompt version** pins all of
it:

| Part | Example |
| --- | --- |
| template and examples | `system.md`, `examples.jsonl` |
| model **snapshot** | a dated model id, not a moving alias such as "latest" |
| parameters | temperature, max tokens, stop sequences, reasoning effort |
| tools | names, descriptions and schemas |
| output schema | the JSON Schema the parser validates against |

Pin the model snapshot. An alias that points to "the newest model" means the provider can
change your product's behaviour without a commit on your side.

## Numbering and changelogs

A semantic-version style works well, with meanings adapted to prompts:

| Change | Bump | Example |
| --- | --- | --- |
| output contract changes (fields, labels, format) | **major** | a new category, a renamed field |
| behaviour changes within the contract | **minor** | new rules, new examples, a new model snapshot |
| wording fixes with no intended behaviour change | **patch** | a typo, clearer phrasing |

A major version means the code that parses the output must change too, so it is deployed
together with that code. Every entry in the changelog says **why** the change was made and what
the evaluation showed: "2.3.0: added examples of long tickets; long-ticket slice 71% → 88%, no
regressions."

## Trace every call

Log, with every model call: the prompt id and version, the model snapshot, the parameters, the
token counts, the stop reason and the latency (and, where privacy allows, the inputs and
outputs). When someone reports a bad answer, you can then tell which version produced it and
reproduce it with the same inputs.

## Rollout and rollback

Ship prompt versions like risky code:

- **evaluate offline first**: the candidate passes the ship rule against the current version;
- **shadow**: run the candidate on live traffic without showing its output, and compare;
- **canary or A/B**: send a small share of traffic to the candidate and watch the metrics;
- **feature flags** select the version at run time, so **rollback** is a configuration change,
  not a deploy;
- keep old versions available until the new one has proven itself.

## Model upgrades and deprecations

Providers release new models and retire old snapshots on a published schedule. Treat a model
change like a dependency upgrade:

1. read the provider's migration notes (some prompting habits change between generations);
2. run your evaluation set on the new snapshot, with the prompt unchanged;
3. analyse the regressions and adapt the prompt where needed (a minor version);
4. roll out gradually, as above.

Do this **before** the deprecation date. A prompt that was tuned for one model is rarely optimal
for the next, and the evaluation set is what makes the migration a routine task instead of a
crisis.

## Further reading

- Semantic Versioning 2.0.0: <https://semver.org/>
- Anthropic, "Model deprecations": <https://docs.anthropic.com/en/docs/about-claude/model-deprecations>
- OpenAI, "Deprecations": <https://platform.openai.com/docs/deprecations>
- OpenAI, "Prompt engineering", reusable prompts and pinning model snapshots: <https://platform.openai.com/docs/guides/prompt-engineering>
- Martin Fowler, "Feature Toggles (aka Feature Flags)": <https://martinfowler.com/articles/feature-toggles.html>
