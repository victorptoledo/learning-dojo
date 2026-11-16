import pytest

from answers import extract_tag, final_answer, majority_vote, normalize, strip_reasoning


def test_extract_tag_takes_the_last_pair_across_lines():
    text = "<answer>draft</answer>\n<thinking>no</thinking>\n<answer>\nfinal\nanswer\n</answer>"
    assert extract_tag(text, "answer") == "final\nanswer"


def test_extract_tag_missing_or_unclosed():
    assert extract_tag("no tags here", "answer") is None
    assert extract_tag("<answer>cut off", "answer") is None
    assert extract_tag("<answer></answer>", "answer") == ""


def test_extract_tag_uses_the_tag_name_literally():
    assert extract_tag("<a.b>x</a.b><aXb>y</aXb>", "a.b") == "x"


def test_strip_reasoning_removes_closed_and_truncated_blocks():
    text = "Intro\n<thinking>step 1\nstep 2</thinking>\n\n\n\nResult\n<thinking>half"
    assert strip_reasoning(text) == "Intro\n\nResult"
    assert strip_reasoning("<thinking>a</thinking>B<thinking>c</thinking>D") == "BD"


def test_final_answer_prefers_the_tag():
    assert final_answer("<thinking>Answer: 7</thinking><answer>8</answer>") == "8"


def test_final_answer_falls_back_to_the_last_answer_line():
    text = "answer: 1\nmore text\n  ANSWER :  3 apples  \ntrailing"
    assert final_answer(text) == "3 apples"


def test_final_answer_never_reads_the_reasoning():
    with pytest.raises(ValueError):
        final_answer("<thinking>Answer: 7</thinking>I am not sure.")
    with pytest.raises(ValueError):
        final_answer("<thinking>Answer: 7 and then the output was cut")


def test_final_answer_empty_tag_falls_back():
    assert final_answer("<answer> </answer>\nAnswer: 5") == "5"
    with pytest.raises(ValueError):
        final_answer("<answer></answer>")


def test_normalize_text():
    assert normalize("  The   Paris. ") == "the paris"
    assert normalize("Straße") == "strasse"
    assert normalize("Done..") == "done."


def test_normalize_numbers():
    assert normalize("1,234.50") == "1234.5"
    assert normalize("42.0") == "42"
    assert normalize("42.") == "42"
    assert normalize("-7") == "-7"
    assert normalize("100") == "100"
    assert normalize("12,34") == "12,34"
    assert normalize("v1.10") == "v1.10"


def test_majority_vote_breaks_ties_by_first_appearance():
    assert majority_vote(["<answer>b</answer>", "<answer>a</answer>", "Answer: a", "Answer: B"]) == ("b", 0.5)


def test_majority_vote_unanimous_and_normalised():
    assert majority_vote(["Answer: 1,000", "<answer>1000.00</answer>", "<answer>1000</answer>"]) == ("1000", 1.0)


def test_majority_vote_with_no_valid_sample():
    with pytest.raises(ValueError):
        majority_vote(["I do not know", "<thinking>Answer: 3"])
    with pytest.raises(ValueError):
        majority_vote([])
