from decimal import Decimal

import pytest

from budget import (
    MESSAGE_OVERHEAD,
    ContextOverflowError,
    Message,
    Price,
    conversation_cost,
    estimate_cost,
    fit_history,
    message_tokens,
    request_tokens,
)
from tokenizer import count_tokens

PRICE = Price(Decimal("3"), Decimal("15"))


def user(text):
    return Message("user", text)


def bot(text):
    return Message("assistant", text)


def test_message_tokens_add_the_overhead():
    assert MESSAGE_OVERHEAD == 4
    assert message_tokens(user("Hello, world!")) == 8
    assert message_tokens(bot("")) == 4


def test_request_tokens_count_system_and_messages():
    messages = [user("one two three"), bot("four")]
    assert request_tokens("Be brief.", messages) == (3 + 4) + (3 + 4) + (1 + 4)
    assert request_tokens("", []) == 4


def history():
    # 8 messages of 1 word each: 5 tokens per message
    return [user("u1"), bot("a1"), user("u2"), bot("a2"), user("u3"), bot("a3"), user("u4"), bot("a4")]


def test_fit_drops_the_oldest_messages():
    # system "Rules" = 1 + 4 = 5 tokens; window 40, reserve 10: 25 tokens for messages = 5 messages
    kept = fit_history("Rules", [*history(), user("u5")], window=40, reserve=10)
    assert [m.content for m in kept] == ["u3", "a3", "u4", "a4", "u5"]


def test_fit_never_starts_with_an_answer():
    # room for 4 messages: a3 u4 a4 u5 -> the leading a3 is dropped
    kept = fit_history("Rules", [*history(), user("u5")], window=35, reserve=10)
    assert [m.content for m in kept] == ["u4", "a4", "u5"]


def test_fit_stops_at_the_first_message_that_does_not_fit():
    long = user("this is a much longer message that cannot fit in the budget at all")
    messages = [user("u0"), long, bot("a1"), user("u2")]
    kept = fit_history("Rules", messages, window=40, reserve=10)
    assert [m.content for m in kept] == ["u2"]


def test_fit_keeps_the_original_order_and_does_not_mutate():
    messages = history()
    copy = list(messages)
    kept = fit_history("Rules", messages, window=1_000, reserve=100)
    assert kept == messages
    assert messages == copy
    assert kept is not messages


def test_fit_exact_budget_fits():
    # budget is exactly 10 tokens: two 5-token messages fit
    kept = fit_history("Rules", [user("u1"), bot("a1")], window=25, reserve=10)
    assert len(kept) == 2


def test_fit_with_no_messages():
    assert fit_history("Rules", [], window=100, reserve=10) == []


def test_fit_raises_when_the_newest_message_does_not_fit():
    newest = user("far too many words for this tiny window to hold at all")
    with pytest.raises(ContextOverflowError) as caught:
        fit_history("Rules", [user("hi"), newest], window=30, reserve=10)
    assert caught.value.needed == message_tokens(newest)
    assert caught.value.available == 15


def test_fit_available_is_never_negative():
    long_system = "word " * 50
    with pytest.raises(ContextOverflowError) as caught:
        fit_history(long_system, [user("hi")], window=30, reserve=10)
    assert caught.value.available == 0


@pytest.mark.parametrize(("window", "reserve"), [(100, -1), (100, 100), (100, 150)])
def test_fit_rejects_bad_reserves(window, reserve):
    with pytest.raises(ValueError):
        fit_history("Rules", [user("hi")], window=window, reserve=reserve)


def test_estimate_cost_rounds_half_up_to_six_places():
    assert estimate_cost(0, 0, PRICE) == Decimal("0")
    assert estimate_cost(1, 0, Price(Decimal("0.5"), Decimal("1"))) == Decimal("0.000001")
    assert estimate_cost(1, 1, PRICE) == Decimal("0.000018")
    result = estimate_cost(2_000_000, 1_000_000, PRICE)
    assert result == Decimal("21")
    assert isinstance(result, Decimal)


def test_estimate_cost_rejects_negative_tokens():
    with pytest.raises(ValueError):
        estimate_cost(-1, 0, PRICE)
    with pytest.raises(ValueError):
        estimate_cost(0, -5, PRICE)


def test_conversation_cost_resends_the_history():
    system = "Be brief."
    turns = [("Hi", "Hello!"), ("What is RAG?", "Retrieval-augmented generation.")]
    first_input = request_tokens(system, [user("Hi")])
    second_input = request_tokens(system, [user("Hi"), bot("Hello!"), user("What is RAG?")])
    expected = estimate_cost(first_input, count_tokens("Hello!"), PRICE) + estimate_cost(
        second_input, count_tokens("Retrieval-augmented generation."), PRICE
    )
    assert conversation_cost(system, turns, PRICE) == expected
    assert conversation_cost(system, [], PRICE) == Decimal("0")


def test_long_conversations_cost_more_than_linearly():
    turns = [("tell me more about it please", "here is a little more about it")] * 40
    price = Price(Decimal("1000000"), Decimal("0"))  # 1 per input token, so cost == input tokens
    short = conversation_cost("Rules", turns[:10], price)
    long = conversation_cost("Rules", turns, price)
    assert long > 4 * short * Decimal("2.5")
