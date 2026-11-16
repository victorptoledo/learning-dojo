import pytest

from prompt import build_prompt, estimate_tokens, fits, render


def test_estimate_tokens():
    assert estimate_tokens("") == 0
    assert estimate_tokens("abcd") == 1
    assert estimate_tokens("abcde") == 2
    assert estimate_tokens("x" * 400) == 100


def test_render_replaces_every_placeholder():
    out = render("Hi {{name}}, from {{ shop }}. Bye {{name}}.", {"name": "Ada", "shop": "Books"})
    assert out == "Hi Ada, from Books. Bye Ada."


def test_render_missing_value_raises_key_error_with_the_name():
    with pytest.raises(KeyError) as error:
        render("Hello {{name}} at {{shop}}", {"name": "Ada"})
    assert "shop" in str(error.value)


def test_render_unused_value_raises_value_error():
    with pytest.raises(ValueError):
        render("Hello {{name}}", {"name": "Ada", "nmae": "typo"})


def test_render_inserts_values_literally():
    out = render("<email>{{email}}</email> {{shop}}", {"email": "see {{shop}}", "shop": "S"})
    assert out == "<email>see {{shop}}</email> S"


def test_render_ignores_single_braces():
    assert render('JSON: {"a": 1} {{x}}', {"x": "ok"}) == 'JSON: {"a": 1} ok'


def test_build_prompt_task_only():
    assert build_prompt("  Say hello.  ") == "<task>\nSay hello.\n</task>"


def test_build_prompt_full_example():
    out = build_prompt(
        "Summarise the email.",
        context="Refunds take 5 days.",
        rules=["At most two sentences", " "],
        output_format="Plain text",
    )
    assert out == (
        "<context>\nRefunds take 5 days.\n</context>\n\n"
        "<task>\nSummarise the email.\n</task>\n\n"
        "<rules>\n- At most two sentences\n</rules>\n\n"
        "<output_format>\nPlain text\n</output_format>"
    )


def test_build_prompt_rules_are_stripped_and_blank_rules_skipped():
    out = build_prompt("T", rules=[" a ", "", "b"])
    assert out == "<task>\nT\n</task>\n\n<rules>\n- a\n- b\n</rules>"
    assert build_prompt("T", rules=["  "]) == "<task>\nT\n</task>"


def test_build_prompt_accepts_any_iterable_of_rules():
    out = build_prompt("T", rules=(r for r in ["one"]))
    assert out.endswith("<rules>\n- one\n</rules>")


def test_build_prompt_blank_task_raises():
    with pytest.raises(ValueError):
        build_prompt("   ", context="something")


def test_fits_with_and_without_reserve():
    prompt = "x" * 40  # 10 tokens
    assert fits(prompt, 10)
    assert not fits(prompt, 9)
    assert fits(prompt, 30, reserve_for_output=20)
    assert not fits(prompt, 29, reserve_for_output=20)
