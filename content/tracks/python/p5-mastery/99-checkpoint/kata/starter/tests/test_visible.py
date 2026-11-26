import io
import json
from datetime import UTC, datetime

from fastapi.testclient import TestClient

from service import OrderRecord, Product, configure_logging, create_app, load_settings


class MemoryRepo:
    def __init__(self):
        self.orders = {}
        self.closed = False

    def open(self):
        pass

    def close(self):
        self.closed = True

    def ping(self):
        return True

    def product(self, sku):
        return {"BOOK-1": Product(1500, 900)}.get(sku)

    def add(self, sku, quantity, product, created_at):
        record = OrderRecord(len(self.orders) + 1, sku, quantity, product.unit_cents, product.cost_cents, created_at)
        self.orders[record.id] = record
        return record

    def get(self, order_id):
        return self.orders.get(order_id)

    def page(self, limit, offset):
        return list(self.orders.values())[offset : offset + limit]


def test_an_order_round_trip_with_a_json_access_log():
    settings = load_settings({"APP_ENV": "test", "DATABASE_URL": "postgresql://app:secret@db/orders"})
    stream = io.StringIO()
    configure_logging(settings, stream=stream)
    repo = MemoryRepo()
    app = create_app(settings, lambda: repo, clock=lambda: datetime(2026, 11, 26, 9, 0, tzinfo=UTC))
    with TestClient(app) as client:
        created = client.post("/orders", json={"sku": "BOOK-1", "quantity": 2}, headers={"X-Request-ID": "r-1"})
    assert created.status_code == 201
    assert created.json()["total_cents"] == 3000
    assert "cost_cents" not in created.json()
    assert repo.closed
    access = [json.loads(line) for line in stream.getvalue().splitlines() if '"service.access"' in line]
    assert access[0]["message"] == "POST /orders 201"
    assert access[0]["request_id"] == "r-1"
