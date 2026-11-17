"""A fresh app on a temporary SQLite file for every test."""

import pytest
from fastapi.testclient import TestClient

from app import create_app


@pytest.fixture
def db_path(tmp_path):
    return str(tmp_path / "tasks.db")


@pytest.fixture
def client(db_path):
    with TestClient(create_app(db_path)) as client:
        yield client
