from grounded import Source, answer_question, build_prompt

REFUND = Source("refunds", "Refund policy", "Refunds are paid within 14 days of the return arriving.", 0.9)


def test_documents_come_before_the_question():
    prompt = build_prompt("How long do refunds take?", [REFUND], budget_tokens=1000)
    user = prompt.messages[1]["content"]
    assert user.index('<document id="refunds"') < user.index("<question>")
    assert prompt.included == ["refunds"]


def test_a_grounded_answer_is_shown():
    reply = '<quotes>\n<quote doc="refunds">paid within 14 days</quote>\n</quotes>\n<answer>Within 14 days.</answer>'
    result = answer_question("How long?", [REFUND], lambda messages: reply, budget_tokens=1000)
    assert (result.status, result.text) == ("answered", "Within 14 days.")
