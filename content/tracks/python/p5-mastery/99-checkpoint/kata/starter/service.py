"""An orders service made ready for production: settings, logs, request ids, lifespan, errors."""

import logging
from collections.abc import Callable, Mapping
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Protocol, TextIO

from fastapi import FastAPI
from pydantic import BaseModel

# ---- given: the storage the service talks to ------------------------------------------------


@dataclass(frozen=True, slots=True)
class Product:
    unit_cents: int
    cost_cents: int  # internal: never leaves the service


@dataclass(frozen=True, slots=True)
class OrderRecord:
    id: int
    sku: str
    quantity: int
    unit_cents: int
    cost_cents: int
    created_at: datetime


class OrderRepository(Protocol):
    def open(self) -> None: ...
    def close(self) -> None: ...
    def ping(self) -> bool: ...
    def product(self, sku: str) -> Product | None: ...
    def add(self, sku: str, quantity: int, product: Product, created_at: datetime) -> OrderRecord: ...
    def get(self, order_id: int) -> OrderRecord | None: ...
    def page(self, limit: int, offset: int) -> list[OrderRecord]: ...


# ---- settings -----------------------------------------------------------------------------


class SettingsError(ValueError):
    """The environment does not describe a valid configuration."""


class Settings(BaseModel):
    """todo: app_env, database_url, max_page_size and log_level, validated."""


def load_settings(env: Mapping[str, str]) -> Settings:
    """Build the settings from environment variables, reporting every bad variable by name."""
    raise NotImplementedError("todo: write load_settings()")


# ---- logging ------------------------------------------------------------------------------


class JsonFormatter(logging.Formatter):
    """One JSON object per line: time, level, logger, message, request_id and exc when present."""

    def format(self, record: logging.LogRecord) -> str:
        raise NotImplementedError("todo: write JsonFormatter.format()")


def configure_logging(settings: Settings, stream: TextIO | None = None) -> None:
    """Send the service's logs as JSON lines to `stream`; safe to call more than once."""
    raise NotImplementedError("todo: write configure_logging()")


# ---- the app ------------------------------------------------------------------------------


def create_app(
    settings: Settings,
    repo_factory: Callable[[], OrderRepository],
    *,
    clock: Callable[[], datetime] = lambda: datetime.now(UTC),
) -> FastAPI:
    """The orders API: /health, POST /orders, GET /orders/{order_id} and GET /orders."""
    raise NotImplementedError("todo: write create_app()")
