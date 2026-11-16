from faq import parse_faq, slugify


def test_slugify():
    assert slugify("How long does delivery take?") == "how-long-does-delivery-take"


def test_parses_one_entry():
    entries = parse_faq("## Can I get a refund?\nYes, within 30 days.")
    assert [(e.id, e.answer) for e in entries] == [("can-i-get-a-refund", "Yes, within 30 days.")]
