"""An orders service made ready for production: settings, logs, request ids, lifespan, errors."""

import json
import logging
import re
import sys
import uuid
from collections.abc import AsyncIterator, Callable, Mapping
from contextlib import asynccontextmanager
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Annotated, Literal, Protocol, Self, TextIO

from fastapi import Depends, FastAPI, HTTPException, Query, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, ConfigDict, Field, SecretStr, ValidationError, field_validator

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


ENV_NAMES = {
    "app_env": "APP_ENV",
    "database_url": "DATABASE_URL",
    "max_page_size": "MAX_PAGE_SIZE",
    "log_level": "LOG_LEVEL",
}


class Settings(BaseModel):
    model_config = ConfigDict(frozen=True)

    app_env: Literal["dev", "test", "prod"]
    database_url: SecretStr
    max_page_size: int = Field(50, ge=1, le=500)
    log_level: Literal["DEBUG", "INFO", "WARNING", "ERROR"] = "INFO"

    @field_validator("database_url")
    @classmethod
    def _postgres_only(cls, value: SecretStr) -> SecretStr:
        if not value.get_secret_value().startswith("postgresql://"):
            raise ValueError("must be a postgresql:// URL")
        return value


def load_settings(env: Mapping[str, str]) -> Settings:
    """Build the settings from environment variables, reporting every bad variable by name."""
    raw = {field: env[name] for field, name in ENV_NAMES.items() if name in env}
    try:
        return Settings(**raw)
    except ValidationError as error:
        names = sorted({ENV_NAMES[str(item["loc"][0])] for item in error.errors()})
        raise SettingsError(f"invalid settings: {', '.join(names)}") from None


# ---- logging ------------------------------------------------------------------------------


class JsonFormatter(logging.Formatter):
    """One JSON object per line: time, level, logger, message, request_id and exc when present."""

    def format(self, record: logging.LogRecord) -> str:
        entry: dict[str, object] = {
            "time": datetime.fromtimestamp(record.created, UTC).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }
        request_id = getattr(record, "request_id", None)
        if request_id is not None:
            entry["request_id"] = request_id
        if record.exc_info:
            entry["exc"] = self.formatException(record.exc_info)
        return json.dumps(entry, ensure_ascii=False)


log = logging.getLogger("service")
access_log = logging.getLogger("service.access")


def configure_logging(settings: Settings, stream: TextIO | None = None) -> None:
    """Send the service's logs as JSON lines to `stream`; safe to call more than once."""
    handler = logging.StreamHandler(stream or sys.stdout)
    handler.setFormatter(JsonFormatter())
    for old in list(log.handlers):
        log.removeHandler(old)
    log.addHandler(handler)
    log.setLevel(settings.log_level)
    log.propagate = False


# ---- API models ---------------------------------------------------------------------------


class OrderIn(BaseModel):
    model_config = ConfigDict(extra="forbid")

    sku: str = Field(pattern=r"^[A-Z0-9-]{3,20}$")
    quantity: int = Field(ge=1, le=100)


class OrderOut(BaseModel):
    id: int
    sku: str
    quantity: int
    unit_cents: int
    total_cents: int
    created_at: datetime

    @classmethod
    def of(cls, record: OrderRecord) -> Self:
        return cls(
            id=record.id,
            sku=record.sku,
            quantity=record.quantity,
            unit_cents=record.unit_cents,
            total_cents=record.unit_cents * record.quantity,
            created_at=record.created_at,
        )


class OrderPage(BaseModel):
    items: list[OrderOut]
    limit: int
    offset: int


# ---- the app ------------------------------------------------------------------------------

_REQUEST_ID = re.compile(r"[A-Za-z0-9-]{1,64}", re.ASCII)


def create_app(
    settings: Settings,
    repo_factory: Callable[[], OrderRepository],
    *,
    clock: Callable[[], datetime] = lambda: datetime.now(UTC),
) -> FastAPI:
    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        repo = repo_factory()
        repo.open()
        app.state.repo = repo
        log.info("service started in %s", settings.app_env)
        try:
            yield
        finally:
            repo.close()
            log.info("service stopped")

    app = FastAPI(lifespan=lifespan)

    def get_repo(request: Request) -> OrderRepository:
        return request.app.state.repo

    Repo = Annotated[OrderRepository, Depends(get_repo)]

    @app.middleware("http")
    async def request_ids(request: Request, call_next):
        incoming = request.headers.get("x-request-id", "")
        request_id = incoming if _REQUEST_ID.fullmatch(incoming) else uuid.uuid4().hex
        try:
            response = await call_next(request)
        except Exception:
            log.exception("unhandled error", extra={"request_id": request_id})
            response = JSONResponse(
                {"detail": "internal error", "request_id": request_id}, status_code=500
            )
        response.headers["X-Request-ID"] = request_id
        access_log.info(
            "%s %s %s", request.method, request.url.path, response.status_code,
            extra={"request_id": request_id},
        )
        return response

    @app.get("/health")
    def health(repo: Repo) -> JSONResponse:
        try:
            ok = repo.ping()
        except Exception:
            ok = False
        if ok:
            return JSONResponse({"status": "ok"})
        return JSONResponse({"status": "unavailable"}, status_code=503)

    @app.post("/orders", status_code=201)
    def create_order(order: OrderIn, repo: Repo) -> OrderOut:
        product = repo.product(order.sku)
        if product is None:
            raise HTTPException(status_code=422, detail="unknown sku")
        return OrderOut.of(repo.add(order.sku, order.quantity, product, clock()))

    @app.get("/orders/{order_id}")
    def read_order(order_id: int, repo: Repo) -> OrderOut:
        record = repo.get(order_id)
        if record is None:
            raise HTTPException(status_code=404, detail="order not found")
        return OrderOut.of(record)

    @app.get("/orders")
    def list_orders(
        repo: Repo,
        limit: Annotated[int | None, Query(ge=1)] = None,
        offset: Annotated[int, Query(ge=0)] = 0,
    ) -> OrderPage:
        if limit is None:
            limit = min(20, settings.max_page_size)
        if limit > settings.max_page_size:
            raise HTTPException(status_code=422, detail=f"limit must be at most {settings.max_page_size}")
        items = [OrderOut.of(record) for record in repo.page(limit, offset)]
        return OrderPage(items=items, limit=limit, offset=offset)

    return app
