from datetime import timedelta

from textkit import highlight, parse_duration, search_key, slugify


def test_the_four_helpers_on_a_catalogue_entry():
    assert search_key("  São  Paulo ") == search_key("SÃO PAULO") == "são paulo"
    assert slugify("São Paulo: Guia 2026!") == "sao-paulo-guia-2026"
    assert highlight("Guide to C++ in São Paulo", ["c++", "paulo"]) == "Guide to [C++] in São [Paulo]"
    assert parse_duration("1h 30m") == timedelta(hours=1, minutes=30)
