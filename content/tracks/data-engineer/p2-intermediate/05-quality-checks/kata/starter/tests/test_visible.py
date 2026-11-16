from checks import not_null, run_checks, unique

ROWS = [
    {"order_id": 1, "customer_id": 10},
    {"order_id": 2, "customer_id": None},
    {"order_id": 2, "customer_id": 11},
]


def test_not_null_reports_the_failing_rows():
    result = not_null("customer_id").evaluate(ROWS)
    assert (result.name, result.passed, result.failing, result.sample) == (
        "not_null(customer_id)", False, 1, (1,))


def test_a_report_with_a_failed_error_does_not_pass():
    report = run_checks(ROWS, [not_null("order_id"), unique("order_id")])
    assert not report.passed
    assert report.summary() == (
        "PASS not_null(order_id)\n"
        "FAIL unique(order_id): 2 failing, rows 1, 2\n"
        "1 passed, 1 failed, 0 warnings"
    )
