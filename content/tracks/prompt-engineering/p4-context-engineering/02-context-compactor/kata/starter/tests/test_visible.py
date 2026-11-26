from context import Turn, check_turns, clear_tool_results


def test_a_call_followed_by_its_result_is_valid():
    turns = [Turn("user", "Fix the bug."), Turn("assistant", "read", call_id="c1"), Turn("tool", "code", call_id="c1")]
    assert check_turns(turns) == []


def test_old_results_become_placeholders():
    turns = [
        Turn("assistant", "read", call_id="c1"),
        Turn("tool", "x" * 40, call_id="c1"),
        Turn("assistant", "read again", call_id="c2"),
        Turn("tool", "y", call_id="c2"),
    ]
    cleared, ids = clear_tool_results(turns, keep_last=1)
    assert ids == ["c1"]
    assert cleared[1] == Turn("tool", "[result cleared: 10 tokens]", call_id="c1")
    assert cleared[3] == turns[3]
