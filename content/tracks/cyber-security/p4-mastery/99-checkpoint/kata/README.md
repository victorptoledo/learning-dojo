# Checkpoint 4: Final Exam: Post-Incident Review Kit

Last week an attacker used a stolen VPN password to reach the document-sharing service and
upload a web shell. The incident is contained. Now the security team has to run the review:
threat model the service again, prove the evidence was not changed, rebuild the timeline
in UTC, meet the notification deadline and plan the fixes.

`review.py` holds the drafts written during the incident. Rewrite its seven functions. Each
part uses one lesson of the Mastery phase and something from earlier in the track.

## Part 1: `stride(model) -> list[tuple[str, str]]` (threat modeling)

```python
model = {
    "elements": {"user": "external", "api": "process", "docs": "store", "audit": "log"},
    "flows": {"f1": ["user", "api"], "f2": ["api", "docs"]},   # id: [from, to]
    "boundaries": [["api", "docs", "audit"]],                 # elements inside each boundary
    "mitigations": [["f1", "T"], ["user", "S"]],              # [element or flow, letter]
}
```

Apply STRIDE per element and return the threats that have **no** mitigation, as
`(name, letter)` pairs sorted by name and then in **STRIDE order** (S, T, R, I, D, E), not
alphabetically:

| Kind | Threats |
| --- | --- |
| `external` | S, R |
| `process` | S, T, R, I, D, E |
| `store` | T, I, D |
| `log` (a data store that is an audit log) | T, R, I, D |
| a flow that **crosses** a trust boundary | T, I, D |
| a flow that does not cross one | none |

A flow crosses a boundary when, for at least one boundary, one end is inside and the other is
not. `boundaries` and `mitigations` may be missing (no boundaries, nothing mitigated).

Raise `ValueError` for a model that is wrong: an unknown kind; a boundary naming an unknown
element; a flow with an unknown end, with the same name as an element, or with **no process
at either end** (in a data flow diagram, data moves between stores and outsiders only
through a process); and a mitigation for a threat that is not in the model (`["docs", "S"]`,
or a letter on a flow that does not cross a boundary): someone mitigated the wrong thing.

## Part 2: evidence and chain of custody (incident response)

- `manifest(root) -> dict[str, str]`: the SHA-256 hex digest of **every** regular file under
  `root` (a `str` or a `Path`), recursively, keyed by the path relative to `root` with `/`
  separators (`"logs/auth.log"`). Memory images are large: hash in chunks or with
  `hashlib.file_digest`, never by reading the whole file at once. Skip symbolic links.
- `check(root, expected) -> dict`: compares the files now under `root` with an earlier
  manifest and returns `{"modified": [...], "missing": [...], "extra": [...]}`, each list
  sorted. Digests compare without regard to case.
- `custody_append(chain, entry) -> list`: returns a **new** list (do not change `chain`)
  with one more record `{"entry": entry, "prev": <hash of the previous record, or 64 zeros
  for the first>, "hash": sha256(prev + canonical(entry))}`, where `canonical(entry)` is
  `json.dumps(entry, sort_keys=True, separators=(",", ":"), ensure_ascii=False)` and the
  text is hashed as UTF-8.
- `custody_verify(chain) -> int | None`: the index of the first record whose `prev` is not
  the hash of the record before it (64 zeros for index 0) or whose `hash` does not match its
  entry; `None` when the whole chain holds. Editing, removing or reordering a record must be
  caught at the first record it breaks.

## Part 3: `timeline(sources, iocs, aware_at) -> dict` (incident response)

```python
sources = [{"name": "vpn", "tz": "America/Sao_Paulo",
            "events": [{"at": "2026-11-20 03:10:00", "ip": "203.0.113.66", "user": "bia", "what": "login"}]}]
iocs = {"ips": ["203.0.113.66"], "hashes": ["<sha256 hex>"]}
```

Each event's `at` is ISO 8601. When it has a UTC offset (`Z`, `+02:00`), use it; when it has
none, it is a wall-clock time in the source's `tz` (an IANA zone name for `zoneinfo`).
Return:

| Key | Value |
| --- | --- |
| `events` | every event as a new dict: its own fields, plus `"source"` (the source name), with `"at"` rewritten in UTC as `"YYYY-MM-DDTHH:MM:SSZ"`. Sorted by the UTC time, then source name, then the order inside the source. |
| `first_seen` | the UTC time (same format) of the earliest event that matches an indicator, or `None` |
| `accounts` | the sorted distinct non-empty `user` values of the matching events |
| `notify_by` | `aware_at` plus 72 hours, in UTC (same format) |

An event matches an indicator when its `ip` is in `iocs["ips"]` or its `sha256` field is in
`iocs["hashes"]` (hex digests compare without regard to case). Either key may be missing.
The 72-hour clock of GDPR (and the short deadline of LGPD) runs from when you **became aware**
of the breach, not from when the attacker arrived. `aware_at` must carry a UTC offset;
raise `ValueError` if it does not. Do not change `sources`.

## Part 4: `remediation_plan(vulns, today) -> list[dict]` (security program)

Each vulnerability is `{"id", "cvss", "kev", "internet_facing", "found", "exception_until"}`
with ISO dates (`"2026-11-01"`) and `exception_until` possibly `None`. Prioritise with the
first row that matches:

| Priority | When | Fix within |
| --- | --- | --- |
| P1 | in CISA KEV **and** internet-facing | 3 days |
| P2 | in KEV, or CVSS ≥ 9.0 | 14 days |
| P3 | CVSS ≥ 7.0 | 30 days |
| P4 | CVSS ≥ 4.0 | 90 days |
| P5 | anything else | 180 days |

`due` is `found` plus the days. The status is `"excepted"` while an exception is active
(`today <= exception_until`, the last day included), otherwise `"overdue"` when
`today > due` (the due day itself is still `"open"`), otherwise `"open"`. Return one
`{"id", "priority", "due", "status"}` per vulnerability (`due` as an ISO date), sorted by
status (overdue, open, excepted), then `due`, then `id`.

Raise `ValueError` when `cvss` is not a number from 0 to 10 (a `bool` or a string is not a
number) or when `kev` or `internet_facing` is not a `bool`: a plan built on a bad feed is
worse than no plan.

Run the visible tests with `uv run pytest`. There are 23 tests: 4 in
`tests/test_visible.py` and 19 hidden ones, plus ruff on `review.py`.
