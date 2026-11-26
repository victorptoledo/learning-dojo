from datetime import datetime, timedelta

from catalog import SHOP
from governance import Lineage, personal_data
from reliability import freshness


def test_lineage_walks_down_from_a_source():
    lineage = Lineage(SHOP)
    assert lineage.downstream("raw.orders") == ["stg_orders", "fct_sales", "daily_revenue"]


def test_personal_data_follows_the_lineage():
    personal = personal_data(Lineage(SHOP), {"raw.customers.email"})
    assert personal == {"raw.customers.email", "stg_customers.email_hash",
                        "dim_customer.email_hash"}


def test_a_product_is_as_fresh_as_its_stalest_input():
    now = datetime(2026, 11, 26, 8, 0)
    loaded = {name: now - timedelta(hours=1) for name in
              ["raw.customers", "raw.orders", "stg_customers", "stg_orders",
               "dim_customer", "fct_sales", "daily_revenue"]}
    loaded["raw.orders"] = now - timedelta(hours=30)
    assert freshness(Lineage(SHOP), loaded, now) == {
        "dim_customer": "ok", "fct_sales": "stale", "daily_revenue": "stale"}
