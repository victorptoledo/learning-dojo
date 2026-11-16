from examples import Example, format_example, pick_examples


def test_format_example():
    assert format_example(Example(" good ", "pos")) == (
        "<example>\n<input>good</input>\n<output>pos</output>\n</example>"
    )


def test_readme_example():
    pool = [Example("good", "pos"), Example("bad", "neg"), Example("great", "pos"),
            Example("awful", "neg"), Example("fine", "pos")]
    assert pick_examples(pool, "GREAT", budget_tokens=1000, max_examples=3) == [
        Example("good", "pos"), Example("bad", "neg"), Example("fine", "pos"),
    ]
