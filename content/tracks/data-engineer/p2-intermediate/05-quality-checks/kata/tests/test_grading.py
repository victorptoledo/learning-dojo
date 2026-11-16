from datetime import UTC, datetime, timedelta

import pytest

from checks import (
    QualityError,
    accepted_values,
    assert_quality,
    freshness,
    in_range,
    not_null,
    relationship,
    row_count,
    run_checks,
    unique,
)

NOW = datetime(2026, 11, 15, 6, 0, tzinfo=UTC)

ORDERS = [
    {"order_id": 1, "customer_id": 10, "status": "paid", "amount": 19.9,
     "loaded_at": datetime(2026, 11, 15, 5, 0, tzinfo=UTC)},
    {"order_id": 2, "customer_id": 11, "status": "PAID", "amount": -5,
     "loaded_at": datetime(2026, 11, 15, 5, 0, tzinfo=UTC)},
    {"order_id": 3, "customer_id": 99, "status": None, "amount": 0,
     "loaded_at": datetime(2026, 11, 15, 5, 30, tzinfo=UTC)},
    {"order_id": 3, "customer_id": None, "status": "shipped", "amount": 1000,
     "loaded_at": None},
    {"order_id": None, "customer_id": 10, "status": "refunded", "amount": None,
     "loaded_at": datetime(2026, 11, 14, 23, 0, tzinfo=UTC)},
]
CUSTOMERS = [{"id": 10}, {"id": 11}, {"id": 12}]


def outcome(check, rows=ORDERS):
    result = check.evaluate(rows)
    return result.passed, result.failing, result.sample


def test_not_null_counts_missing_keys_as_null():
    assert outcome(not_null("order_id")) == (False, 1, (4,))
    assert outcome(not_null("discount")) == (False, 5, (0, 1, 2))


def test_not_null_keeps_falsy_values():
    assert outcome(not_null("amount"), [{"amount": 0}, {"amount": ""}]) == (True, 0, ())


def test_unique_fails_every_duplicated_row_and_ignores_nulls():
    assert outcome(unique("order_id")) == (False, 2, (2, 3))
    assert outcome(unique("order_id"), [{"order_id": None}, {"order_id": None}]) == (True, 0, ())


def test_unique_over_several_columns():
    rows = [{"a": 1, "b": 1}, {"a": 1, "b": 2}, {"a": 1, "b": 1}]
    assert unique("a", "b").name == "unique(a, b)"
    assert outcome(unique("a", "b"), rows) == (False, 2, (0, 2))


def test_unique_needs_a_column():
    with pytest.raises(ValueError):
        unique()


def test_accepted_values_is_exact_and_leaves_nulls_to_not_null():
    check = accepted_values("status", ["paid", "shipped", "refunded"])
    assert check.name == "accepted_values(status)"
    assert outcome(check) == (False, 1, (1,))


def test_in_range_is_inclusive_and_open_ended():
    assert outcome(in_range("amount", 0, 1000)) == (False, 1, (1,))
    assert outcome(in_range("amount", low=0)) == (False, 1, (1,))
    assert outcome(in_range("amount", high=100)) == (False, 1, (3,))
    assert in_range("amount", 0).name == "in_range(amount)"


def test_relationship_finds_orphans():
    check = relationship("customer_id", CUSTOMERS, "id")
    assert check.name == "relationship(customer_id -> id)"
    assert outcome(check) == (False, 1, (2,))


def test_relationship_accepts_a_generator_of_parents():
    check = relationship("customer_id", (c for c in CUSTOMERS), "id")
    assert outcome(check, ORDERS[:2]) == (True, 0, ())
    assert outcome(check, ORDERS[:2]) == (True, 0, ())


def test_the_sample_is_the_first_three_failing_rows():
    rows = [{"x": None}] * 5
    assert outcome(not_null("x"), rows) == (False, 5, (0, 1, 2))


def test_row_count_checks_the_whole_batch():
    assert row_count(1, 5).name == "row_count(1..5)"
    assert outcome(row_count(1, 5)) == (True, 0, ())
    assert outcome(row_count(1, 4)) == (False, 1, ())
    assert outcome(row_count(1, 5), []) == (False, 1, ())


def test_freshness_looks_at_the_newest_value():
    assert freshness("loaded_at", timedelta(hours=1), NOW).name == "freshness(loaded_at)"
    assert outcome(freshness("loaded_at", timedelta(hours=1), NOW)) == (True, 0, ())
    assert outcome(freshness("loaded_at", timedelta(minutes=29), NOW)) == (False, 1, ())
    assert outcome(freshness("loaded_at", timedelta(minutes=30), NOW)) == (True, 0, ())


def test_freshness_fails_without_any_timestamp():
    assert outcome(freshness("loaded_at", timedelta(days=1), NOW), []) == (False, 1, ())
    rows = [{"loaded_at": None}]
    assert outcome(freshness("loaded_at", timedelta(days=1), NOW), rows) == (False, 1, ())


def test_severity_is_error_or_warn():
    assert not_null("x").severity == "error"
    assert not_null("x", severity="warn").severity == "warn"
    with pytest.raises(ValueError):
        not_null("x", severity="fatal")
    with pytest.raises(ValueError):
        row_count(1, 2, severity="info")


def test_warnings_do_not_block():
    report = run_checks(ORDERS, [not_null("order_id", severity="warn"),
                                 row_count(1, 10)])
    assert report.passed
    assert [r.name for r in report.warnings] == ["not_null(order_id)"]
    assert report.failures == []


def test_results_keep_the_order_of_the_checks():
    checks = [row_count(1, 10), not_null("order_id"), unique("order_id")]
    report = run_checks(ORDERS, checks)
    assert [r.name for r in report.results] == ["row_count(1..10)", "not_null(order_id)",
                                                "unique(order_id)"]
    assert [r.name for r in report.failures] == ["not_null(order_id)", "unique(order_id)"]


def test_run_checks_accepts_any_iterable_of_checks():
    report = run_checks(ORDERS, (c for c in [row_count(1, 10)]))
    assert len(report.results) == 1


def test_the_summary_of_a_mixed_report():
    report = run_checks(ORDERS, [
        unique("order_id"),
        in_range("amount", 0, severity="warn"),
        relationship("customer_id", CUSTOMERS, "id"),
        row_count(1, 3),
        not_null("status", severity="warn"),
        freshness("loaded_at", timedelta(hours=2), NOW),
    ])
    assert report.summary() == (
        "FAIL unique(order_id): 2 failing, rows 2, 3\n"
        "WARN in_range(amount): 1 failing, rows 1\n"
        "FAIL relationship(customer_id -> id): 1 failing, rows 2\n"
        "FAIL row_count(1..3)\n"
        "WARN not_null(status): 1 failing, rows 2\n"
        "PASS freshness(loaded_at)\n"
        "1 passed, 3 failed, 2 warnings"
    )


def test_assert_quality_returns_the_report_when_it_passes():
    report = assert_quality(ORDERS[:2], [unique("order_id"), not_null("status")])
    assert report.passed


def test_assert_quality_raises_with_the_report():
    with pytest.raises(QualityError) as caught:
        assert_quality(ORDERS, [unique("order_id")])
    assert not caught.value.report.passed
    assert "FAIL unique(order_id)" in str(caught.value)
