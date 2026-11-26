# Checkpoint 3: Release Gate

Before every deploy, the platform team runs `gate.py` over what is about to ship: the IAM
policy of the service's cloud role, the CI workflow that deploys it, the authentication
events of the last hours, and the log lines the service writes. Today each check catches
one case in ten. Rewrite the four functions so the gate catches what this phase taught.

All four are pure functions of their arguments: no files, no network, no clock.

## Part 1: `iam_findings(policy) -> list[str]`

`policy` is an AWS IAM policy document (a `dict` as `json.load` returns it). `Statement` is
a list of statements **or a single statement object**. `Action`, `NotAction`, `Resource`
and `Principal["AWS"]` may each be a string or a list of strings. Action names are
case-insensitive (`IAM:PassRole` is `iam:passrole`).

`Effect` must be exactly `"Allow"` or `"Deny"`; anything else, including a missing
`Effect`, raises `ValueError` (fail closed: a gate that cannot read a policy must not pass
it). `Deny` statements produce no findings. For each `Allow` statement, at index `i`
(starting at 0), add these findings, **in this order**:

| Finding | When |
| --- | --- |
| `i:admin` | an action is exactly `*` |
| `i:action-wildcard` | otherwise, an action contains `*` (`s3:*`, `ec2:Describe*`) |
| `i:not-action` | the statement uses `NotAction` (Allow + NotAction allows everything else) |
| `i:passrole-any` | it allows `iam:PassRole` with a resource of `*`: anyone with it can hand any role, admin included, to a service they control |
| `i:public` | the `Principal` is `"*"` or its `AWS` entry includes `"*"`, and there is no non-empty `Condition` |

Return the findings in statement order.

## Part 2: `workflow_findings(workflow) -> list[str]`

`workflow` is a GitHub Actions workflow as a YAML parser returns it. Return the **sorted**
list of distinct findings:

| Finding | When |
| --- | --- |
| `token-permissions` | the top-level `permissions` key is missing or is `"write-all"`, or any job sets `permissions: write-all`. An empty mapping `{}` is the strictest setting, not a missing one. |
| `unpinned:<name>` | a step's `uses` is not pinned. A remote action (`owner/repo@ref` or `owner/repo/path@ref`) is pinned only by a full 40-character hex commit SHA; tags, branches, short SHAs and a missing `@ref` are not. `docker://` images are pinned only by `@sha256:` and 64 lowercase hex digits. Local actions (`./...`) are always fine. `<name>` is the `uses` value before the first `@`. |
| `script-injection:<job>` | a step's `run` script contains `${{` followed (after optional spaces) by `github.event.` or `github.head_ref`: an issue title or branch name controlled by an outsider is pasted into a shell script. |
| `untrusted-checkout` | the workflow is triggered by `pull_request_target` (it runs with the base repository's secrets) and a step uses `actions/checkout` with a `with.ref` that mentions `github.event.pull_request.head` or `github.head_ref`: the fork's code now runs with those secrets. |
| `long-lived-key:<job>` | a step passes an `aws-access-key-id` input (the name is case-insensitive): use OIDC federation to a role instead. |

The triggers in `on` can be a string, a list or a mapping. Watch out: YAML 1.1 parsers such
as PyYAML read the key `on` as the boolean `True`, so look for both `"on"` and `True`.

## Part 3: `detect(events, *, window=600, spray_users=5, brute_failures=10, failures_before_success=3)`

`events` is a list of authentication events `{"ts": int, "ip": str, "user": str,
"outcome": "success" | "failure"}`, **in any order**. Process them by `ts` (events with the
same `ts` keep their order in the list). An event belongs to the window of an event at time
`now` when `now - ts < window`, counting the event itself. Raise `ValueError` for any other
`outcome`.

| Alert | Fires when |
| --- | --- |
| `("spraying", ip, ts)` | a failure brings the number of **distinct users** that one IP failed against inside the window to `spray_users` |
| `("brute-force", user, ts)` | a failure brings the failures for one user inside the window, **from any IP**, to `brute_failures` |
| `("success-after-failures", user, ts)` | a success arrives for a user with at least `failures_before_success` failures inside the window before it |

A success clears that user's failures, as a lockout counter would. Each alert fires **once**
per IP or user (for its kind), at the `ts` of the event that triggers it. Return the alerts
sorted by `ts`, then kind, then the IP or user.

## Part 4: `log_line(event) -> str`

Write one log line: the event as JSON with sorted keys, `separators=(",", ":")` and only
ASCII characters, so that a newline or a right-to-left override in a username cannot forge
a second line or hide text. Replace the value of every key whose lower-cased name contains
`password`, `secret`, `token`, `authorization`, `cookie` or `session` by `"[REDACTED]"`, at
any depth (inside nested objects and lists). Do not change the caller's `event`.

```python
>>> log_line({"user": "ada", "password": "hunter2"})
'{"password":"[REDACTED]","user":"ada"}'
```

Run the visible tests with `uv run pytest`. There are 30 tests: 4 in
`tests/test_visible.py` and 26 hidden ones, plus ruff on `gate.py`.
