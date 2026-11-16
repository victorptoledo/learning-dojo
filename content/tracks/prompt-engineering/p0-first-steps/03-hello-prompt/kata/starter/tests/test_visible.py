from prompt import build_prompt, estimate_tokens


def test_estimate_rounds_up():
    assert estimate_tokens("abcde") == 2


def test_task_only():
    assert build_prompt("Say hello.") == "<task>\nSay hello.\n</task>"
