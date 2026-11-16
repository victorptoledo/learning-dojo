from answers import extract_tag, majority_vote


def test_extract_a_tag():
    assert extract_tag("<thinking>2+2</thinking><answer> 4 </answer>", "answer") == "4"


def test_readme_vote():
    samples = ["<answer>42</answer>", "Answer: 42.0", "<answer>41</answer>", "no idea"]
    assert majority_vote(samples) == ("42", 2 / 3)
