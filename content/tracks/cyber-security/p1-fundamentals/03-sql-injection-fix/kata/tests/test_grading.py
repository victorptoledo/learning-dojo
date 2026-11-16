import pytest

from seed import demo_db
from store import find_user, products_by_ids, rename_product, search_products


def names(rows):
    return [row["name"] for row in rows]


def table_count(conn, table):
    return conn.execute(f"SELECT count(*) FROM {table}").fetchone()[0]


def test_honest_lookups_still_work():
    conn = demo_db()
    assert find_user(conn, "ada") == {"id": 1, "username": "ada", "role": "admin"}
    assert find_user(conn, "nobody") is None
    assert find_user(conn, "o'brien") is None


def test_find_user_is_not_injectable():
    conn = demo_db()
    assert find_user(conn, "' OR '1'='1") is None
    assert find_user(conn, "' OR role = 'admin' --") is None
    assert find_user(conn, "x' UNION SELECT 99, 'evil', 'admin' --") is None


def test_honest_search_and_sort_orders():
    conn = demo_db()
    assert names(search_products(conn, "mug")) == ["Blue Mug", "Mug Rack", "Red Mug"]
    assert names(search_products(conn, "Mug", sort="price")) == ["Red Mug", "Blue Mug", "Mug Rack"]
    assert names(search_products(conn, "mug", sort="price", descending=True)) == [
        "Mug Rack", "Blue Mug", "Red Mug",
    ]
    # equal created_at: Red Mug (id 2) before Green Tea (id 6)
    assert names(search_products(conn, "", sort="newest"))[2:4] == ["Red Mug", "Green Tea"]
    assert search_products(conn, "tea") == [{"id": 6, "name": "Green Tea", "price_cents": 900}]


def test_search_terms_are_data():
    conn = demo_db()
    assert search_products(conn, "' OR '1'='1") == []
    assert search_products(conn, "x' UNION SELECT id, username, role FROM users --") == []
    assert search_products(conn, "x'; DROP TABLE products; --") == []
    assert table_count(conn, "products") == 7


def test_like_wildcards_match_literally():
    conn = demo_db()
    assert names(search_products(conn, "%")) == ["100% Cotton Tee"]
    assert names(search_products(conn, "_")) == ["snake_case Sticker"]
    assert names(search_products(conn, "e_c")) == ["snake_case Sticker"]
    assert names(search_products(conn, "\\")) == ["Back\\slash Poster"]
    assert search_products(conn, "M%g") == []


def test_unknown_sorts_are_rejected():
    conn = demo_db()
    for sort in ["id", "price_cents", "name; DROP TABLE products", "(SELECT 1)", ""]:
        with pytest.raises(ValueError):
            search_products(conn, "mug", sort=sort)
    assert table_count(conn, "products") == 7


def test_limit_is_validated_and_clamped():
    conn = demo_db()
    assert len(search_products(conn, "", limit=2)) == 2
    assert len(search_products(conn, "", limit=0)) == 1
    assert len(search_products(conn, "", limit=-5)) == 1
    assert len(search_products(conn, "", limit=10_000)) == 7
    for limit in ["5", "1; DROP TABLE products", 2.0, True, None]:
        with pytest.raises(ValueError):
            search_products(conn, "", limit=limit)
    assert table_count(conn, "products") == 7


def test_products_by_ids():
    conn = demo_db()
    assert names(products_by_ids(conn, [3, 1])) == ["Blue Mug", "Mug Rack"]
    assert products_by_ids(conn, []) == []
    assert products_by_ids(conn, [42]) == []


def test_products_by_ids_binds_each_value():
    conn = demo_db()
    assert products_by_ids(conn, ["1) OR (1=1"]) == []
    assert products_by_ids(conn, ["0) UNION SELECT id, username, role FROM users --"]) == []


def test_rename_product_binds_both_values():
    conn = demo_db()
    assert rename_product(conn, 6, "O'Hara's Tea") is True
    assert names(search_products(conn, "o'hara")) == ["O'Hara's Tea"]
    assert rename_product(conn, 99, "Ghost") is False
    assert rename_product(conn, 1, "pwned', price_cents = 0 --") is True
    prices = {row["id"]: row["price_cents"] for row in search_products(conn, "", limit=100)}
    assert prices[2] == 1100
    assert prices[3] == 3500


def test_rename_cannot_target_other_rows():
    conn = demo_db()
    assert rename_product(conn, "1 OR 1=1", "Everything") is False
    assert names(search_products(conn, "everything")) == []
