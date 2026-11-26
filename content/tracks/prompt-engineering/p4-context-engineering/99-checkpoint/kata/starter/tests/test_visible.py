from optimiser import Example, extract_candidate, score


def test_score_is_the_share_of_passing_examples():
    examples = [Example("a", "refund", "billing"), Example("b", "crash", "technical")]
    result = score("Classify: {{input}}", examples, lambda messages: " billing ", lambda got, expected: got == expected)
    assert result.rate == 0.5
    assert [failure.id for failure in result.failures] == ["b"]


def test_a_candidate_must_keep_the_placeholder():
    assert extract_candidate("<prompt>Classify: {{input}}</prompt>", ["input"]) == "Classify: {{input}}"
