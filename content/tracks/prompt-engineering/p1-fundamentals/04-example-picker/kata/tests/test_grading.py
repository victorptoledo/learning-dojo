from examples import Example, estimate_tokens, few_shot_block, format_example, label_counts, pick_examples


def cost(example: Example) -> int:
    text = f"<example>\n<input>{example.input.strip()}</input>\n<output>{example.output.strip()}</output>\n</example>"
    return (len(text) + 3) // 4


POOL = [
    Example("good", "pos"),
    Example("bad", "neg"),
    Example("great", "pos"),
    Example("awful", "neg"),
    Example("fine", "pos"),
    Example("so-so", "mixed"),
]


def test_estimate_tokens():
    assert estimate_tokens("") == 0
    assert estimate_tokens("abcdefghi") == 3


def test_format_example_strips():
    assert format_example(Example("  a b \n", " lbl ")) == (
        "<example>\n<input>a b</input>\n<output>lbl</output>\n</example>"
    )


def test_round_robin_interleaves_labels_in_order_of_first_appearance():
    picked = pick_examples(POOL, "unrelated", budget_tokens=10_000, max_examples=6)
    assert [e.output for e in picked] == ["pos", "neg", "mixed", "pos", "neg", "pos"]
    assert [e.input for e in picked] == ["good", "bad", "so-so", "great", "awful", "fine"]


def test_default_maximum_is_four():
    assert len(pick_examples(POOL, "x", budget_tokens=10_000)) == 4


def test_query_is_never_an_example_ignoring_case_and_spacing():
    pool = [Example("Fast  delivery", "pos"), Example("slow", "neg"), Example("quick", "pos")]
    picked = pick_examples(pool, "  fast DELIVERY ", budget_tokens=10_000)
    assert Example("Fast  delivery", "pos") not in picked
    assert picked == [Example("slow", "neg"), Example("quick", "pos")]


def test_duplicates_keep_the_first():
    pool = [Example("Same", "a"), Example("same ", "b"), Example("other", "b")]
    assert pick_examples(pool, "q", budget_tokens=10_000) == [Example("Same", "a"), Example("other", "b")]


def test_budget_is_respected():
    budget = cost(POOL[0]) + cost(POOL[1])
    picked = pick_examples(POOL, "x", budget_tokens=budget, max_examples=6)
    assert picked == [POOL[0], POOL[1]]
    assert sum(cost(e) for e in picked) <= budget


def test_an_example_that_does_not_fit_is_skipped_and_smaller_ones_still_fit():
    long = Example("word " * 200, "pos")
    short = Example("ok", "pos")
    neg = Example("no", "neg")
    budget = cost(short) + cost(neg)
    assert pick_examples([long, neg, short], "x", budget_tokens=budget) == [neg, short]


def test_zero_budget_or_empty_pool():
    assert pick_examples(POOL, "x", budget_tokens=0) == []
    assert pick_examples([], "x", budget_tokens=100) == []
    assert pick_examples(POOL, "x", budget_tokens=100, max_examples=0) == []


def test_few_shot_block():
    assert few_shot_block([]) == ""
    block = few_shot_block([Example("a", "x"), Example("b", "y")])
    assert block == (
        "<examples>\n"
        "<example>\n<input>a</input>\n<output>x</output>\n</example>\n"
        "<example>\n<input>b</input>\n<output>y</output>\n</example>\n"
        "</examples>"
    )


def test_label_counts_in_order_of_first_appearance():
    counts = label_counts(POOL)
    assert counts == {"pos": 3, "neg": 2, "mixed": 1}
    assert list(counts) == ["pos", "neg", "mixed"]
    assert label_counts(e for e in []) == {}


def test_picked_set_is_balanced():
    pool = [Example(f"p{i}", "pos") for i in range(10)] + [Example(f"n{i}", "neg") for i in range(2)]
    counts = label_counts(pick_examples(pool, "x", budget_tokens=10_000, max_examples=4))
    assert counts == {"pos": 2, "neg": 2}
