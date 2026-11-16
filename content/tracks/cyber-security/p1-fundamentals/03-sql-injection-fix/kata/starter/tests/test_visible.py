from seed import demo_db
from store import find_user, search_products


def test_finds_a_user_by_name():
    assert find_user(demo_db(), "grace") == {"id": 2, "username": "grace", "role": "customer"}


def test_searching_with_a_quote_neither_crashes_nor_leaks():
    assert search_products(demo_db(), "' OR '1'='1") == []
