from datetime import date, datetime, timedelta

import pytest

from catalog import SHOP, Column, Grant, Model
from governance import Lineage, erasure_targets, personal_data, read
from reliability import backfill_plan, blast_radius, error_budget, freshness

ORDER = ["raw.customers", "raw.orders", "stg_customers", "dim_customer", "stg_orders",
         "fct_sales", "daily_revenue"]
TAGS = {"raw.customers.customer_id", "raw.customers.email", "raw.customers.name",
        "raw.orders.customer_id"}
NOW = datetime(2026, 11, 26, 8, 0)


@pytest.fixture
def lineage():
    return Lineage(SHOP)


@pytest.fixture
def personal(lineage):
    return personal_data(lineage, TAGS)


# ---- lineage -----------------------------------------------------------------------------

def test_order_is_topological_with_alphabetical_ties(lineage):
    assert lineage.order() == ORDER


def test_order_follows_dependencies_over_the_alphabet():
    lineage = Lineage([Model("a_mart", upstream=("z_raw",)), Model("z_raw"), Model("m_raw")])
    assert lineage.order() == ["m_raw", "z_raw", "a_mart"]
    sources = Lineage([Model(name) for name in "edcba"])
    assert sources.order() == ["a", "b", "c", "d", "e"]


def test_models_may_come_in_any_iterable():
    lineage = Lineage(model for model in reversed(SHOP))
    assert lineage.order() == ORDER
    assert set(lineage.models) == set(ORDER)


def test_upstream_and_downstream_are_transitive_and_in_order(lineage):
    assert lineage.upstream("fct_sales") == ["raw.customers", "raw.orders", "stg_customers",
                                             "dim_customer", "stg_orders"]
    assert lineage.downstream("stg_customers") == ["dim_customer", "fct_sales",
                                                   "daily_revenue"]
    assert lineage.downstream("daily_revenue") == []
    assert lineage.upstream("raw.orders") == []


def test_unknown_models_are_a_key_error(lineage):
    with pytest.raises(KeyError):
        lineage.downstream("nope")


@pytest.mark.parametrize(("models", "message"), [
    ([Model("a"), Model("a")], "duplicate"),
    ([Model("a", columns=(Column("x"), Column("x")))], "duplicate"),
    ([Model("b", upstream=("a",))], "unknown"),
    ([Model("a", columns=(Column("x"),)),
      Model("b", upstream=("a",), columns=(Column("y", ("a.nope",)),))], "unknown"),
    ([Model("a", columns=(Column("x"),)), Model("c", columns=(Column("x"),)),
      Model("b", upstream=("a",), columns=(Column("y", ("c.x",)),))], "unknown"),
    ([Model("a", upstream=("b",)), Model("b", upstream=("a",))], "cycle"),
    ([Model("a", upstream=("a",))], "cycle"),
])
def test_a_bad_catalog_is_refused(models, message):
    with pytest.raises(ValueError, match=message):
        Lineage(models)


def test_column_origins_reach_the_sources_through_dotted_model_names(lineage):
    assert lineage.origins("dim_customer.display_name") == [
        "raw.customers.country", "raw.customers.name"]
    assert lineage.origins("daily_revenue.country") == ["raw.customers.country"]
    assert lineage.origins("raw.orders.amount_cents") == ["raw.orders.amount_cents"]
    with pytest.raises(KeyError):
        lineage.origins("raw.orders.nope")


def test_column_impact_lists_every_column_built_from_it(lineage):
    assert lineage.impact("raw.customers.country") == [
        "daily_revenue.country", "dim_customer.country", "dim_customer.display_name",
        "fct_sales.country", "stg_customers.country"]
    assert lineage.impact("raw.orders.amount_cents") == [
        "daily_revenue.revenue_cents", "fct_sales.amount_cents", "stg_orders.amount_cents"]
    assert lineage.impact("daily_revenue.revenue_cents") == []


def test_impact_does_not_confuse_columns_with_the_same_name(lineage):
    assert "fct_sales.customer_id" not in lineage.impact("raw.customers.customer_id")
    assert "fct_sales.customer_id" in lineage.impact("raw.orders.customer_id")


# ---- personal data -----------------------------------------------------------------------

def test_personal_data_propagates_through_copies_derivations_and_hashes(personal):
    assert personal == {
        "raw.customers.customer_id", "raw.customers.email", "raw.customers.name",
        "raw.orders.customer_id",
        "stg_customers.customer_id", "stg_customers.email_hash", "stg_customers.name",
        "stg_orders.customer_id",
        "dim_customer.customer_id", "dim_customer.email_hash", "dim_customer.display_name",
        "fct_sales.customer_id",
    }


def test_a_hash_is_still_personal_data_but_an_aggregate_is_not(personal):
    assert "dim_customer.email_hash" in personal
    assert "daily_revenue.customers" not in personal


def test_only_source_columns_can_be_tagged(lineage):
    with pytest.raises(ValueError):
        personal_data(lineage, {"dim_customer.country"})
    with pytest.raises(KeyError):
        personal_data(lineage, {"raw.customers.phone"})


def test_tags_reach_a_derived_column_through_any_of_its_sources():
    lineage = Lineage([
        Model("raw.a", columns=(Column("safe"), Column("secret"))),
        Model("mart", upstream=("raw.a",), columns=(
            Column("mixed", ("raw.a.safe", "raw.a.secret"), "derive"),)),
    ])
    assert personal_data(lineage, {"raw.a.secret"}) == {"raw.a.secret", "mart.mixed"}


def test_erasure_rewrites_every_model_with_personal_data_in_order(lineage, personal):
    assert erasure_targets(lineage, personal) == [
        "raw.customers", "raw.orders", "stg_customers", "dim_customer", "stg_orders",
        "fct_sales"]


# ---- access ------------------------------------------------------------------------------

def dim_rows():
    return [
        {"customer_id": 1, "email_hash": "9f86", "display_name": "Ada (GB)", "country": "GB"},
        {"customer_id": 2, "email_hash": "60b7", "display_name": "Bia (BR)", "country": "BR"},
    ]


GRANTS = {
    "analyst": Grant(models=frozenset({"dim_customer", "fct_sales", "daily_revenue"})),
    "support": Grant(models=frozenset({"dim_customer"}), pii=True),
    "manager_br": Grant(models=frozenset({"dim_customer", "stg_orders"}),
                        row_filter=("country", "BR")),
}


def test_personal_columns_are_masked_for_roles_without_pii(lineage, personal):
    assert read(lineage, GRANTS, "analyst", "dim_customer", dim_rows(), personal) == [
        {"customer_id": None, "email_hash": None, "display_name": None, "country": "GB"},
        {"customer_id": None, "email_hash": None, "display_name": None, "country": "BR"},
    ]


def test_a_pii_role_sees_everything_it_was_granted(lineage, personal):
    assert read(lineage, GRANTS, "support", "dim_customer", dim_rows(), personal) == dim_rows()


def test_reading_never_changes_the_rows_given(lineage, personal):
    rows = dim_rows()
    read(lineage, GRANTS, "analyst", "dim_customer", rows, personal)
    read(lineage, GRANTS, "manager_br", "dim_customer", rows, personal)
    assert rows == dim_rows()


def test_row_filters_keep_only_matching_rows(lineage, personal):
    assert read(lineage, GRANTS, "manager_br", "dim_customer", dim_rows(), personal) == [
        {"customer_id": None, "email_hash": None, "display_name": None, "country": "BR"}]


def test_a_row_filter_on_a_missing_column_fails_closed(lineage, personal):
    rows = [{"order_id": 1, "customer_id": 2, "amount_cents": 10, "order_date": "2026-11-01"}]
    assert read(lineage, GRANTS, "manager_br", "stg_orders", rows, personal) == []


def test_access_is_denied_by_default(lineage, personal):
    with pytest.raises(PermissionError):
        read(lineage, GRANTS, "support", "fct_sales", [], personal)
    with pytest.raises(PermissionError):
        read(lineage, GRANTS, "intern", "daily_revenue", [], personal)


def test_aggregates_are_not_masked(lineage, personal):
    rows = [{"order_date": "2026-11-01", "country": "BR", "customers": 3,
             "revenue_cents": 9000}]
    assert read(lineage, GRANTS, "analyst", "daily_revenue", rows, personal) == rows


# ---- freshness and incidents -------------------------------------------------------------

def fresh(**overrides):
    loaded = {name: NOW - timedelta(hours=1) for name in ORDER}
    loaded.update({name.replace("__", "."): NOW - timedelta(hours=hours)
                   for name, hours in overrides.items()})
    return loaded


def test_all_products_fresh(lineage):
    assert freshness(lineage, fresh(), NOW) == {
        "dim_customer": "ok", "fct_sales": "ok", "daily_revenue": "ok"}


def test_a_stale_source_makes_every_product_on_top_of_it_stale(lineage):
    assert freshness(lineage, fresh(raw__customers=25), NOW) == {
        "dim_customer": "stale", "fct_sales": "stale", "daily_revenue": "stale"}


def test_each_product_uses_its_own_objective_and_the_limit_is_inclusive(lineage):
    assert freshness(lineage, fresh(stg_orders=6), NOW)["fct_sales"] == "ok"
    report = freshness(lineage, fresh(fct_sales=7), NOW)
    assert report == {"dim_customer": "ok", "fct_sales": "stale", "daily_revenue": "ok"}


def test_a_model_never_loaded_makes_its_products_unknown(lineage):
    loaded = fresh()
    del loaded["stg_orders"]
    assert freshness(lineage, loaded, NOW) == {
        "dim_customer": "ok", "fct_sales": "unknown", "daily_revenue": "unknown"}


def test_the_blast_radius_names_products_and_owners(lineage):
    assert blast_radius(lineage, "stg_customers") == {
        "models": ["stg_customers", "dim_customer", "fct_sales", "daily_revenue"],
        "products": ["dim_customer", "fct_sales", "daily_revenue"],
        "owners": ["analytics", "finance"],
    }
    assert blast_radius(lineage, "daily_revenue") == {
        "models": ["daily_revenue"], "products": ["daily_revenue"], "owners": ["finance"]}
    assert blast_radius(lineage, "raw.orders")["owners"] == ["analytics", "finance"]


def test_a_backfill_rebuilds_downstream_models_day_by_day_in_order(lineage):
    assert backfill_plan(lineage, "stg_orders", date(2026, 11, 1), date(2026, 11, 2)) == [
        ("stg_orders", date(2026, 11, 1)), ("fct_sales", date(2026, 11, 1)),
        ("daily_revenue", date(2026, 11, 1)),
        ("stg_orders", date(2026, 11, 2)), ("fct_sales", date(2026, 11, 2)),
        ("daily_revenue", date(2026, 11, 2)),
    ]


def test_a_backfill_never_reruns_upstream_models(lineage):
    plan = backfill_plan(lineage, "dim_customer", date(2026, 11, 1), date(2026, 11, 1))
    assert [model for model, _ in plan] == ["dim_customer", "fct_sales", "daily_revenue"]
    with pytest.raises(ValueError):
        backfill_plan(lineage, "fct_sales", date(2026, 11, 2), date(2026, 11, 1))


# ---- error budgets -----------------------------------------------------------------------

def test_an_error_budget_counts_allowed_and_missed_days():
    days = [True] * 97 + [False] * 3
    assert error_budget(days, 0.99) == {"sli": 0.97, "allowed": 1, "missed": 3,
                                        "remaining": -2}


def test_error_budgets_are_exact_without_floating_point_errors():
    assert error_budget([True] * 30, 0.9)["allowed"] == 3
    assert error_budget([True] * 5, 0.8)["allowed"] == 1
    assert error_budget([True] * 28 + [False] * 2, 0.9)["remaining"] == 1


def test_an_error_budget_needs_days_and_a_sensible_objective():
    with pytest.raises(ValueError):
        error_budget([], 0.99)
    with pytest.raises(ValueError):
        error_budget([True], 1.5)
    assert error_budget([True, False], 1)["remaining"] == -1
