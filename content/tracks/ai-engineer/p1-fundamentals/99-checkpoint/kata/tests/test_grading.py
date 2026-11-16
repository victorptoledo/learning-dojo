import json

import pytest

from embedder import embed
from faq import Entry, FaqFormatError, FaqSearch, Match, parse_faq, slugify
from tokenizer import count_tokens

# The same text as faq.md, so editing the sample does not change the grading.
FAQ = """\
# Orders

## How long does delivery take?
tags: shipping, delivery

Most parcels are delivered within 3 to 5 days.

International delivery takes up to 2 weeks because of customs.

## Can I get a refund?
tags: billing, returns

You can return any item within 30 days and get your money back.

## How do I cancel an order?

Orders can be cancelled until they are shipped.

# Account

## How do I reset my password?
tags: account

Use "Forgot password" on the login page.

## Can I pay by card or PayPal?
tags: billing

We accept every major card and PayPal. Your card is charged when the order ships.
"""


def search():
    return FaqSearch(parse_faq(FAQ))


# ---- parsing ---------------------------------------------------------------------------------


def test_slugify_strips_punctuation_and_case():
    assert slugify("Can I pay by card or PayPal?") == "can-i-pay-by-card-or-paypal"
    assert slugify("  2FA: why, how?  ") == "2fa-why-how"


def test_parse_the_sample():
    entries = parse_faq(FAQ)
    assert [e.id for e in entries] == [
        "how-long-does-delivery-take",
        "can-i-get-a-refund",
        "how-do-i-cancel-an-order",
        "how-do-i-reset-my-password",
        "can-i-pay-by-card-or-paypal",
    ]
    delivery = entries[0]
    assert delivery.section == "Orders"
    assert delivery.question == "How long does delivery take?"
    assert delivery.tags == ("shipping", "delivery")
    assert delivery.answer == (
        "Most parcels are delivered within 3 to 5 days.\n\n"
        "International delivery takes up to 2 weeks because of customs."
    )
    assert entries[2].tags == ()
    assert entries[3].section == "Account"


def test_parse_collapses_whitespace_and_normalises_tags():
    text = "## Q one?\n\ntags:  Billing , ,RETURNS \n\nLine   one\ncontinues.\n\n\n  Second.  \n"
    (entry,) = parse_faq(text)
    assert entry.section == ""
    assert entry.tags == ("billing", "returns")
    assert entry.answer == "Line one continues.\n\nSecond."


def test_parse_errors_have_line_numbers():
    with pytest.raises(FaqFormatError) as caught:
        parse_faq("# Orders\n\nstray text\n## Q?\nA.")
    assert caught.value.line == 3
    with pytest.raises(FaqFormatError) as caught:
        parse_faq("## First?\nAnswer.\n\n## Empty?\n\ntags: x\n\n## Third?\nAnswer.")
    assert caught.value.line == 4
    with pytest.raises(FaqFormatError) as caught:
        parse_faq("## Same?\nOne.\n# Other\n## Same?\nTwo.")
    assert caught.value.line == 4
    assert isinstance(caught.value, ValueError)


def test_parse_empty_text():
    assert parse_faq("") == []
    assert parse_faq("# Only a section\n\n") == []


# ---- search ------------------------------------------------------------------------------------


def test_search_finds_entries_by_meaning():
    matches = search().search("I want my money back")
    assert matches[0].entry_id == "can-i-get-a-refund"
    assert matches[0].question == "Can I get a refund?"
    assert matches[0].score == pytest.approx(1.0)


def test_search_returns_entries_not_chunks():
    matches = search().search("how many days until my parcel arrives", k=5, min_score=0.1)
    ids = [m.entry_id for m in matches]
    assert ids[0] == "how-long-does-delivery-take"
    assert len(ids) == len(set(ids))


def test_search_threshold_and_k():
    engine = search()
    loose = engine.search("when will my card be charged for delivery", k=5, min_score=0.0)
    assert len(loose) >= 3
    assert engine.search("when will my card be charged for delivery", k=2, min_score=0.0) == loose[:2]
    strict = engine.search("when will my card be charged for delivery", k=5, min_score=0.99)
    assert all(m.score >= 0.99 for m in strict)


def test_search_scores_are_sorted_with_ties_by_id():
    matches = search().search("refund delivery password", k=5, min_score=0.0)
    keys = [(-m.score, m.entry_id) for m in matches]
    assert keys == sorted(keys)


def test_search_section_filter():
    matches = search().search("pay by card", section="Orders", min_score=0.0)
    assert "can-i-pay-by-card-or-paypal" not in [m.entry_id for m in matches]
    account = search().search("pay by card", section="Account")
    assert [m.entry_id for m in account] == ["can-i-pay-by-card-or-paypal"]


def test_search_with_no_concepts_finds_nothing():
    assert search().search("the weather is nice today") == []


def test_search_skips_texts_without_concepts():
    entries = [Entry("hello", "", "Hello?", "Nice to meet you.")]
    engine = FaqSearch(entries)
    assert engine.search("refund") == []


def test_search_uses_the_given_embedder():
    calls = []

    def spy(text):
        calls.append(text)
        return embed(text)

    engine = FaqSearch(parse_faq(FAQ), embed=spy)
    engine.search("refund please")
    assert "Can I get a refund?" in calls
    assert calls[-1] == "refund please"


def test_entry_lookup():
    engine = search()
    assert engine.entry("can-i-get-a-refund").section == "Orders"
    with pytest.raises(KeyError):
        engine.entry("nope")


# ---- context and answer --------------------------------------------------------------------


def test_build_context_fits_the_budget():
    engine = search()
    matches = [Match("can-i-get-a-refund", "Can I get a refund?", 1.0), Match("how-do-i-cancel-an-order", "How do I cancel an order?", 0.5)]
    first = "Q: Can I get a refund?\nA: You can return any item within 30 days and get your money back."
    second = "Q: How do I cancel an order?\nA: Orders can be cancelled until they are shipped."
    both = first + "\n\n" + second
    assert engine.build_context(matches, count_tokens(both)) == both
    assert engine.build_context(matches, count_tokens(both) - 1) == first
    assert engine.build_context(matches, 3) == ""
    assert engine.build_context([], 100) == ""


def test_build_context_stops_at_the_first_block_that_does_not_fit():
    engine = search()
    long_first = Match("how-long-does-delivery-take", "How long does delivery take?", 1.0)
    short_second = Match("how-do-i-cancel-an-order", "How do I cancel an order?", 0.9)
    assert engine.build_context([long_first, short_second], 20) == ""


def test_answer_with_a_match():
    result = search().answer("I forgot my password and cannot log in")
    assert result["status"] == "answered"
    assert result["entry"] == "how-do-i-reset-my-password"
    assert result["question"] == "How do I reset my password?"
    assert result["answer"] == 'Use "Forgot password" on the login page.'
    assert result["score"] == round(result["score"], 3)
    assert result["query"] == "I forgot my password and cannot log in"
    assert "how-do-i-reset-my-password" not in result["related"]
    json.dumps(result)


def test_answer_related_lists_the_other_matches():
    engine = search()
    result = engine.answer("refund or cancel my order", min_score=0.3)
    expected = [m.entry_id for m in engine.search("refund or cancel my order", k=3, min_score=0.3)]
    assert result["entry"] == expected[0]
    assert result["related"] == expected[1:]


def test_answer_without_a_match():
    assert search().answer("tell me a joke") == {"status": "no_match", "query": "tell me a joke"}
