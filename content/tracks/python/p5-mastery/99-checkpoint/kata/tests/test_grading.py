import io
import json
import logging
import sys
from datetime import UTC, datetime

import pytest
from fastapi.testclient import TestClient

from service import (
    JsonFormatter,
    OrderRecord,
    Product,
    Settings,
    SettingsError,
    configure_logging,
    create_app,
    load_settings,
)

SECRET_URL = "postgresql://orders:hunter2@db.internal/orders"
ENV = {"APP_ENV": "test", "DATABASE_URL": SECRET_URL}
NOW = datetime(2026, 11, 26, 9, 0, tzinfo=UTC)


class FakeRepo:
    def __init__(self):
        self.opened = 0
        self.closed = 0
        self.healthy = True
        self.broken = False
        self.catalogue = {"BOOK-1": Product(1500, 900), "PEN-22": Product(250, 40)}
        self.orders: dict[int, OrderRecord] = {}

    def open(self):
        self.opened += 1

    def close(self):
        self.closed += 1

    def ping(self):
        if self.healthy is None:
            raise ConnectionError("db down")
        return self.healthy

    def product(self, sku):
        return self.catalogue.get(sku)

    def add(self, sku, quantity, product, created_at):
        record = OrderRecord(len(self.orders) + 1, sku, quantity, product.unit_cents, product.cost_cents, created_at)
        self.orders[record.id] = record
        return record

    def get(self, order_id):
        if self.broken:
            raise RuntimeError(f"connection reset while reading {SECRET_URL}")
        return self.orders.get(order_id)

    def page(self, limit, offset):
        return list(self.orders.values())[offset : offset + limit]


@pytest.fixture
def repo():
    return FakeRepo()


@pytest.fixture
def logs():
    stream = io.StringIO()
    configure_logging(load_settings(ENV), stream=stream)

    def lines():
        return [json.loads(line) for line in stream.getvalue().splitlines()]

    return lines


def client_for(repo, env=ENV, **kwargs):
    app = create_app(load_settings(env), lambda: repo, clock=lambda: NOW)
    return TestClient(app, raise_server_exceptions=False, **kwargs)


# ---- settings -----------------------------------------------------------------------------


def test_settings_from_the_environment_with_defaults():
    settings = load_settings(ENV)
    assert isinstance(settings, Settings)
    assert settings.app_env == "test"
    assert settings.database_url.get_secret_value() == SECRET_URL
    assert settings.max_page_size == 50
    assert settings.log_level == "INFO"
    custom = load_settings({**ENV, "MAX_PAGE_SIZE": "5", "LOG_LEVEL": "DEBUG"})
    assert (custom.max_page_size, custom.log_level) == (5, "DEBUG")


def test_the_database_url_never_shows_in_repr_or_str():
    settings = load_settings(ENV)
    assert "hunter2" not in repr(settings)
    assert "hunter2" not in str(settings)


def test_every_bad_variable_is_reported_by_name_without_its_value():
    env = {"APP_ENV": "staging", "DATABASE_URL": "mysql://root:hunter2@db/x", "MAX_PAGE_SIZE": "lots"}
    with pytest.raises(SettingsError) as caught:
        load_settings(env)
    message = str(caught.value)
    assert "APP_ENV" in message and "DATABASE_URL" in message and "MAX_PAGE_SIZE" in message
    assert "LOG_LEVEL" not in message
    assert "hunter2" not in message and "staging" not in message
    assert caught.value.__cause__ is None or "hunter2" not in str(caught.value.__cause__)
    assert isinstance(caught.value, ValueError)


def test_missing_variables_and_out_of_range_values_are_errors():
    with pytest.raises(SettingsError, match="DATABASE_URL"):
        load_settings({"APP_ENV": "prod"})
    with pytest.raises(SettingsError, match="MAX_PAGE_SIZE"):
        load_settings({**ENV, "MAX_PAGE_SIZE": "0"})
    with pytest.raises(SettingsError, match="LOG_LEVEL"):
        load_settings({**ENV, "LOG_LEVEL": "LOUD"})


# ---- logging ------------------------------------------------------------------------------


def record(message, *args, **extra):
    rec = logging.LogRecord("service.x", logging.WARNING, __file__, 1, message, args, None)
    rec.created = NOW.timestamp()
    for key, value in extra.items():
        setattr(rec, key, value)
    return rec


def test_json_formatter_writes_one_object_per_line():
    line = JsonFormatter().format(record("paid %s for %s", "€12", "Reunião", request_id="abc"))
    assert "\n" not in line
    assert json.loads(line) == {
        "time": "2026-11-26T09:00:00+00:00",
        "level": "WARNING",
        "logger": "service.x",
        "message": "paid €12 for Reunião",
        "request_id": "abc",
    }
    assert "Reunião" in line


def test_json_formatter_includes_the_traceback_when_there_is_one():
    try:
        raise KeyError("sku")
    except KeyError:
        rec = record("failed")
        rec.exc_info = sys.exc_info()
    entry = json.loads(JsonFormatter().format(rec))
    assert "request_id" not in entry
    assert "Traceback" in entry["exc"] and "KeyError" in entry["exc"]


def test_configure_logging_twice_does_not_duplicate_lines():
    stream = io.StringIO()
    settings = load_settings(ENV)
    configure_logging(settings, stream=stream)
    configure_logging(settings, stream=stream)
    logging.getLogger("service").info("hello")
    assert len(stream.getvalue().splitlines()) == 1


def test_service_logs_do_not_also_go_to_the_root_logger(caplog):
    configure_logging(load_settings(ENV), stream=io.StringIO())
    with caplog.at_level(logging.INFO):
        logging.getLogger("service").warning("only once")
    assert caplog.records == []


def test_configure_logging_respects_the_level():
    stream = io.StringIO()
    configure_logging(load_settings({**ENV, "LOG_LEVEL": "WARNING"}), stream=stream)
    logging.getLogger("service.access").info("quiet")
    logging.getLogger("service").warning("loud")
    assert [json.loads(line)["message"] for line in stream.getvalue().splitlines()] == ["loud"]


# ---- lifespan and health ------------------------------------------------------------------


def test_the_repository_is_opened_once_at_startup_and_closed_at_shutdown(repo, logs):
    with client_for(repo) as client:
        assert (repo.opened, repo.closed) == (1, 0)
        client.get("/health")
        client.get("/health")
        assert repo.opened == 1
    assert repo.closed == 1
    assert not any("hunter2" in json.dumps(entry) for entry in logs())


def test_health_reports_the_database(repo):
    with client_for(repo) as client:
        assert (client.get("/health").status_code, client.get("/health").json()) == (200, {"status": "ok"})
        repo.healthy = False
        response = client.get("/health")
        assert (response.status_code, response.json()) == (503, {"status": "unavailable"})
        repo.healthy = None  # ping raises
        assert client.get("/health").status_code == 503


# ---- orders -------------------------------------------------------------------------------


def test_create_an_order_without_leaking_the_cost_price(repo):
    with client_for(repo) as client:
        response = client.post("/orders", json={"sku": "BOOK-1", "quantity": 3})
    assert response.status_code == 201
    assert response.json() == {
        "id": 1,
        "sku": "BOOK-1",
        "quantity": 3,
        "unit_cents": 1500,
        "total_cents": 4500,
        "created_at": "2026-11-26T09:00:00Z",
    }
    assert repo.orders[1].created_at == NOW


@pytest.mark.parametrize(
    "body",
    [
        {"sku": "BOOK-1", "quantity": 0},
        {"sku": "BOOK-1", "quantity": 101},
        {"sku": "book-1", "quantity": 1},
        {"sku": "BOOK-1"},
        {"sku": "BOOK-1", "quantity": 1, "cost_cents": 1},
    ],
)
def test_invalid_bodies_are_422_and_store_nothing(repo, body):
    with client_for(repo) as client:
        response = client.post("/orders", json=body)
    assert response.status_code == 422
    assert isinstance(response.json()["detail"], list)  # FastAPI's validation error, not ours
    assert repo.orders == {}


def test_an_unknown_sku_is_422(repo):
    with client_for(repo) as client:
        response = client.post("/orders", json={"sku": "NOPE-1", "quantity": 1})
    assert response.status_code == 422
    assert response.json() == {"detail": "unknown sku"}


def test_read_an_order_and_a_missing_one(repo):
    with client_for(repo) as client:
        client.post("/orders", json={"sku": "PEN-22", "quantity": 2})
        found = client.get("/orders/1")
        missing = client.get("/orders/99")
        bad = client.get("/orders/abc")
    assert found.status_code == 200
    assert "cost_cents" not in found.json()
    assert found.json()["total_cents"] == 500
    assert (missing.status_code, missing.json()) == (404, {"detail": "order not found"})
    assert bad.status_code == 422


def test_pages_respect_the_configured_maximum(repo):
    with client_for(repo, env={**ENV, "MAX_PAGE_SIZE": "3"}) as client:
        for _ in range(5):
            client.post("/orders", json={"sku": "PEN-22", "quantity": 1})
        first = client.get("/orders").json()
        second = client.get("/orders", params={"limit": 2, "offset": 3}).json()
        too_big = client.get("/orders", params={"limit": 4})
        negative = client.get("/orders", params={"offset": -1})
    assert (first["limit"], first["offset"], [o["id"] for o in first["items"]]) == (3, 0, [1, 2, 3])
    assert [o["id"] for o in second["items"]] == [4, 5]
    assert too_big.status_code == 422
    assert negative.status_code == 422


def test_the_default_page_is_twenty(repo):
    with client_for(repo) as client:
        for _ in range(25):
            client.post("/orders", json={"sku": "PEN-22", "quantity": 1})
        page = client.get("/orders").json()
    assert page["limit"] == 20 and len(page["items"]) == 20


# ---- request ids, access log and errors ---------------------------------------------------


def test_a_safe_incoming_request_id_is_echoed_and_logged(repo, logs):
    with client_for(repo) as client:
        response = client.get("/orders/7", headers={"X-Request-ID": "req-42"})
    assert response.headers["X-Request-ID"] == "req-42"
    access = [e for e in logs() if e["logger"] == "service.access"]
    assert access == [{**access[0], "message": "GET /orders/7 404", "request_id": "req-42", "level": "INFO"}]


def test_a_missing_or_unsafe_request_id_is_replaced(repo, logs):
    with client_for(repo) as client:
        plain = client.get("/health")
        hostile = client.get("/health", headers={"X-Request-ID": "x\" injected=1 " + "a" * 80})
    ids = [plain.headers["X-Request-ID"], hostile.headers["X-Request-ID"]]
    assert all(len(rid) >= 16 and rid.replace("-", "").isalnum() for rid in ids)
    assert ids[0] != ids[1]
    assert "injected" not in json.dumps(logs())


def test_an_unexpected_error_is_a_500_without_details_and_is_logged_with_its_traceback(repo, logs):
    repo.broken = True
    with client_for(repo) as client:
        response = client.get("/orders/1", headers={"X-Request-ID": "boom-1"})
        assert client.get("/health").status_code == 200  # the service keeps working
    assert response.status_code == 500
    assert response.json() == {"detail": "internal error", "request_id": "boom-1"}
    assert "hunter2" not in response.text and "Traceback" not in response.text
    assert response.headers["X-Request-ID"] == "boom-1"
    errors = [e for e in logs() if e["level"] == "ERROR"]
    assert len(errors) == 1
    assert errors[0]["request_id"] == "boom-1"
    assert "RuntimeError" in errors[0]["exc"]
    access = [e["message"] for e in logs() if e["logger"] == "service.access"]
    assert "GET /orders/1 500" in access
