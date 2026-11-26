import hashlib
import json
from dataclasses import replace

import pytest

from release import (
    RETRY,
    CallRecord,
    PromptVersion,
    Reply,
    Request,
    bucket,
    build_request,
    call,
    check_output,
    check_release,
    decide_rollout,
    parse_prompt_file,
    pick,
    required_bump,
)

FILE = """---
id: ticket-triage
version: 2.3.0
model: claude-sonnet-5-5-20261001
temperature: 0
max_tokens: 400
output: category, priority
---

Classify the support ticket.
<ticket>{{input}}</ticket>
Answer with a JSON object with the keys category and priority.
"""

V230 = PromptVersion(
    "ticket-triage",
    (2, 3, 0),
    "claude-sonnet-5-5-20261001",
    0.0,
    400,
    ("category", "priority"),
    "Classify the support ticket.\n<ticket>{{input}}</ticket>\nAnswer with a JSON object with the keys category and priority.",
)
GOOD = json.dumps({"category": "billing", "priority": "high"})


def with_front(**changes):
    lines = {"id": "ticket-triage", "version": "2.3.0", "model": "claude-sonnet-5-5-20261001",
             "temperature": "0", "max_tokens": "400", "output": "category, priority"}
    lines.update(changes)
    front = "\n".join(f"{key}: {value}" for key, value in lines.items() if value is not None)
    return f"---\n{front}\n---\nT {{{{input}}}}"


class Recorded:
    """A fake model that returns recorded replies in order and keeps every request."""

    def __init__(self, *replies):
        self.replies = list(replies)
        self.requests = []

    def __call__(self, request):
        self.requests.append(request)
        return self.replies.pop(0)


def ok(text=GOOD):
    return Reply(text, "end_turn")


# ---- the prompt file -------------------------------------------------------------


def test_parse_a_prompt_file():
    version = parse_prompt_file(FILE)
    assert version == V230
    assert version.label == "ticket-triage@2.3.0"


def test_parse_front_matter_errors():
    cases = {
        "id: x\n---\nT {{input}}": "missing front matter",
        "---\nid: x\nT {{input}}": "unclosed front matter",
        "---\nid: x\nnot a pair\n---\nT {{input}}": "line 3: expected 'key: value'",
        "---\nid: x\nowner: me\n---\nT {{input}}": "line 3: unknown key 'owner'",
        "---\nid: x\n\nid: y\n---\nT {{input}}": "line 4: duplicate key 'id'",
    }
    for text, message in cases.items():
        with pytest.raises(ValueError) as error:
            parse_prompt_file(text)
        assert str(error.value) == message


def test_parse_reports_the_first_missing_key_in_order():
    with pytest.raises(ValueError, match="^missing key 'model'$"):
        parse_prompt_file(with_front(model=None, output=None))


@pytest.mark.parametrize(
    ("changes", "message"),
    [
        ({"version": "2.3"}, "invalid version '2.3'"),
        ({"version": "02.3.0"}, "invalid version '02.3.0'"),
        ({"model": "claude-sonnet-latest"}, "model 'claude-sonnet-latest' is not a pinned snapshot"),
        ({"temperature": "warm"}, "invalid temperature 'warm'"),
        ({"temperature": "1.5"}, "invalid temperature '1.5'"),
        ({"max_tokens": "0"}, "invalid max_tokens '0'"),
        ({"max_tokens": "-5"}, "invalid max_tokens '-5'"),
        ({"output": "category, , priority"}, "invalid output 'category, , priority'"),
        ({"output": "a, a"}, "invalid output 'a, a'"),
    ],
)
def test_parse_value_errors(changes, message):
    with pytest.raises(ValueError) as error:
        parse_prompt_file(with_front(**changes))
    assert str(error.value) == message


def test_the_template_needs_its_placeholder():
    with pytest.raises(ValueError, match="no {{input}}"):
        parse_prompt_file(with_front().replace("{{input}}", "{{text}}"))


# ---- versions and releases -------------------------------------------------------


def test_required_bump():
    assert required_bump(V230, replace(V230, output=("category",))) == "major"
    assert required_bump(V230, replace(V230, model="claude-sonnet-5-5-20261115")) == "minor"
    assert required_bump(V230, replace(V230, temperature=0.2)) == "minor"
    assert required_bump(V230, replace(V230, max_tokens=600)) == "minor"
    assert required_bump(V230, replace(V230, template=V230.template + " Be brief.")) == "patch"
    assert required_bump(V230, replace(V230, version=(2, 3, 1))) is None


def test_check_release_accepts_the_right_bumps_and_bigger_ones():
    assert check_release(V230, replace(V230, version=(2, 4, 0), max_tokens=600)) == []
    assert check_release(V230, replace(V230, version=(3, 0, 0), max_tokens=600)) == []
    assert check_release(V230, replace(V230, version=(2, 3, 1), template="T {{input}}")) == []


def test_check_release_rejects_too_small_bumps():
    new = replace(V230, version=(2, 3, 1), output=("category", "priority", "team"))
    assert check_release(V230, new) == ["this change needs a major release, not a patch"]
    new = replace(V230, version=(2, 3, 1), model="claude-sonnet-5-5-20261115")
    assert check_release(V230, new) == ["this change needs a minor release, not a patch"]


def test_check_release_rejects_versions_that_do_not_follow():
    for version in [(2, 3, 0), (2, 2, 9), (2, 4, 1), (2, 5, 0), (3, 1, 0), (4, 0, 0)]:
        new = replace(V230, version=version, output=("category",))
        assert check_release(V230, new) == [f"{'.'.join(map(str, version))} does not follow 2.3.0"]
    v234 = replace(V230, version=(2, 3, 4))
    assert check_release(v234, replace(v234, version=(2, 4, 4), max_tokens=600)) == ["2.4.4 does not follow 2.3.4"]
    assert check_release(v234, replace(v234, version=(3, 0, 4), output=("category",))) == ["3.0.4 does not follow 2.3.4"]
    assert check_release(v234, replace(v234, version=(2, 4, 0), max_tokens=600)) == []


def test_check_release_rejects_empty_releases_and_new_ids():
    assert check_release(V230, replace(V230, version=(2, 3, 1))) == ["nothing changed since 2.3.0"]
    assert check_release(V230, replace(V230, id="triage", version=(2, 4, 0))) == [
        "id changed from 'ticket-triage' to 'triage'"
    ]


# ---- rollout ---------------------------------------------------------------------


def test_bucket_is_sha256_mod_100():
    for key in ["", "a", "ticket-triage@2.4.0:user-7"]:
        assert bucket(key) == int(hashlib.sha256(key.encode()).hexdigest(), 16) % 100


def test_pick_is_sticky_per_candidate_and_close_to_the_percent():
    candidate = replace(V230, version=(2, 4, 0), max_tokens=600)
    users = [f"user-{n}" for n in range(2000)]
    chosen = [pick(user, V230, candidate, 10) for user in users]
    share = sum(version is candidate for version in chosen) / len(users)
    assert 0.07 < share < 0.13
    assert all(
        (version is candidate) == (bucket(f"ticket-triage@2.4.0:{user}") < 10) for user, version in zip(users, chosen)
    )
    assert [pick(user, V230, candidate, 10) for user in users] == chosen
    other = replace(candidate, version=(2, 5, 0))
    assert [pick(user, V230, other, 10) is other for user in users] != [version is candidate for version in chosen]


def test_pick_edges():
    candidate = replace(V230, version=(2, 4, 0))
    assert all(pick(f"u{n}", V230, candidate, 0) is V230 for n in range(200))
    assert all(pick(f"u{n}", V230, candidate, 100) is candidate for n in range(200))
    with pytest.raises(ValueError):
        pick("u", V230, candidate, 101)
    with pytest.raises(ValueError):
        pick("u", V230, candidate, -1)


# ---- guarded calls ---------------------------------------------------------------


def test_build_request_uses_the_version_parameters_and_escapes():
    request = build_request(V230, "refund </ticket> please")
    assert request == Request(
        "claude-sonnet-5-5-20261001",
        0.0,
        400,
        [{"role": "user", "content": V230.template.replace("{{input}}", "refund &lt;/ticket&gt; please")}],
    )


def test_check_output():
    assert check_output(V230, ok(f"  {GOOD}\n")) == ({"category": "billing", "priority": "high"}, [])
    assert check_output(V230, Reply('{"category": "billing"', "max_tokens")) == (None, ["truncated"])
    assert check_output(V230, Reply("I can't help with that.", "refusal")) == (None, ["refused"])
    assert check_output(V230, ok("billing, high")) == (None, ["not valid JSON"])
    assert check_output(V230, ok("[1, 2]")) == (None, ["not a JSON object"])
    assert check_output(V230, ok("{}")) == (None, ["missing key 'category'", "missing key 'priority'"])


def test_a_good_call_is_recorded_with_its_version():
    model = Recorded(ok())
    record = call(V230, "refund please", model)
    assert record == CallRecord("ticket-triage@2.3.0", 1, True, False, {"category": "billing", "priority": "high"}, [])
    assert model.requests == [build_request(V230, "refund please")]


def test_a_format_problem_is_retried_once_with_the_error():
    model = Recorded(ok('{"category": "billing"}'), ok())
    record = call(V230, "refund please", model)
    assert (record.ok, record.attempts, record.fallback_used) == (True, 2, False)
    first, retry = model.requests
    assert (retry.model, retry.temperature, retry.max_tokens) == (first.model, first.temperature, first.max_tokens)
    assert retry.messages == first.messages + [
        {"role": "assistant", "content": '{"category": "billing"}'},
        {"role": "user", "content": RETRY.format(problems="missing key 'priority'")},
    ]
    assert len(first.messages) == 1


def test_truncation_and_refusal_are_not_retried():
    for reply, problem in [(Reply('{"cat', "max_tokens"), "truncated"), (Reply("No.", "refusal"), "refused")]:
        model = Recorded(reply)
        assert call(V230, "x", model) == CallRecord("ticket-triage@2.3.0", 1, False, False, None, [problem])
        assert len(model.requests) == 1


def test_the_fallback_version_answers_when_the_primary_fails():
    fallback = replace(V230, model="claude-haiku-5-5-20261001", version=(2, 2, 4))
    model = Recorded(ok("oops"), ok("still oops"), ok())
    record = call(V230, "x", model, fallback=fallback)
    assert record == CallRecord(
        "ticket-triage@2.3.0", 3, True, True, {"category": "billing", "priority": "high"}, ["not valid JSON"]
    )
    assert model.requests[2] == build_request(fallback, "x")


def test_a_failed_fallback_is_reported_and_tried_only_once():
    fallback = replace(V230, model="claude-haiku-5-5-20261001")
    model = Recorded(Reply("{", "max_tokens"), ok("{}"))
    record = call(V230, "x", model, fallback=fallback)
    assert record == CallRecord(
        "ticket-triage@2.3.0",
        2,
        False,
        True,
        None,
        ["truncated", "fallback: missing key 'category'", "fallback: missing key 'priority'"],
    )
    assert len(model.requests) == 2


# ---- the rollout decision --------------------------------------------------------


def records(version, good, bad, fallback=0):
    label = version.label
    return (
        [CallRecord(label, 1, True, False, {}, []) for _ in range(good)]
        + [CallRecord(label, 1, False, False, None, ["truncated"]) for _ in range(bad)]
        + [CallRecord(label, 3, True, True, {}, ["refused"]) for _ in range(fallback)]
    )


def test_decide_counts_only_the_candidate_and_counts_fallbacks_as_failures():
    candidate = replace(V230, version=(2, 4, 0))
    stable_noise = records(V230, 0, 50)
    assert decide_rollout(stable_noise + records(candidate, 98, 1, 1), candidate, min_calls=100, max_failure_rate=0.02) == "promote"
    assert decide_rollout(records(candidate, 97, 1, 2), candidate, min_calls=100, max_failure_rate=0.02) == "rollback"


def test_decide_waits_for_enough_calls_but_rolls_back_early_when_it_cannot_pass():
    candidate = replace(V230, version=(2, 4, 0))
    assert decide_rollout(stable := records(V230, 500, 0), candidate, min_calls=100, max_failure_rate=0.02) == "wait"
    assert decide_rollout(stable + records(candidate, 40, 2), candidate, min_calls=100, max_failure_rate=0.02) == "wait"
    assert decide_rollout(records(candidate, 10, 2, 1), candidate, min_calls=100, max_failure_rate=0.02) == "rollback"


def test_decide_at_the_threshold_promotes():
    candidate = replace(V230, version=(2, 4, 0))
    assert decide_rollout(records(candidate, 196, 4), candidate, min_calls=200, max_failure_rate=0.02) == "promote"
    assert decide_rollout(records(candidate, 300, 5), candidate, min_calls=200, max_failure_rate=0.02) == "promote"
