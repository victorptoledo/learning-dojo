# Typed Web Services with FastAPI and Pydantic

Much of the Python running in production is an HTTP service. **FastAPI** shows what modern
Python does well: the type hints you already write become request validation, response
serialisation and API documentation, with no extra schema language.

## WSGI and ASGI

| | WSGI | ASGI |
| --- | --- | --- |
| Model | One synchronous call per request | Async: requests, WebSockets, streaming, lifespan events |
| Frameworks | Django (classic), Flask | FastAPI, Starlette, Litestar, Django (async views) |
| Servers | Gunicorn, uWSGI | **Uvicorn**, Hypercorn, Granian |

FastAPI is an ASGI framework built on Starlette (HTTP) and Pydantic (data):

```text
uv add "fastapi[standard]"
uv run fastapi dev app/main.py      # Uvicorn with reload; open /docs for Swagger UI
```

## Path operations are typed functions

```python
from fastapi import FastAPI, HTTPException, status
from pydantic import BaseModel, Field

app = FastAPI(title="Orders")

class OrderIn(BaseModel):
    sku: str = Field(min_length=1)
    quantity: int = Field(ge=1, le=100)

class OrderOut(BaseModel):
    id: int
    sku: str
    quantity: int
    status: str

@app.post("/orders", status_code=status.HTTP_201_CREATED)
async def create_order(order: OrderIn) -> OrderOut:          # body parsed into OrderIn
    saved = await repo.add(order)
    return OrderOut.model_validate(saved, from_attributes=True)

@app.get("/orders/{order_id}")
async def get_order(order_id: int, include_lines: bool = False) -> OrderOut:
    order = await repo.get(order_id)
    if order is None:
        raise HTTPException(status_code=404, detail="order not found")
    return OrderOut.model_validate(order, from_attributes=True)
```

From the signatures alone FastAPI knows that `order_id` is a path parameter and must be an
int, `include_lines` is an optional query parameter, and the body must match `OrderIn`.
Invalid input is rejected with **422** and a precise error list before your code runs. The
**return type** (or `response_model=`) filters and serialises the output, so internal fields
never leak, and everything appears in the generated OpenAPI document at `/openapi.json`.

## Dependency injection

`Depends` declares something an endpoint needs; FastAPI builds it per request, and
dependencies can depend on others. A dependency with `yield` gets cleanup:

```python
from typing import Annotated
from fastapi import Depends, Header

async def get_session() -> AsyncIterator[AsyncSession]:
    async with SessionLocal() as session:   # opened per request
        yield session                       # closed after the response

async def current_user(authorization: Annotated[str, Header()],
                       session: Annotated[AsyncSession, Depends(get_session)]) -> User:
    user = await users.from_token(session, authorization.removeprefix("Bearer "))
    if user is None:
        raise HTTPException(status_code=401, detail="invalid token")
    return user

Session = Annotated[AsyncSession, Depends(get_session)]
CurrentUser = Annotated[User, Depends(current_user)]

@app.get("/me/orders")
async def my_orders(user: CurrentUser, session: Session) -> list[OrderOut]: ...
```

Within one request a dependency is evaluated once and shared (the session above is the same
object for `current_user` and `my_orders`). Group endpoints with `APIRouter` and
`app.include_router(router, prefix="/orders")`.

## async def or def?

| Endpoint | Runs | Use it when |
| --- | --- | --- |
| `async def` | On the event loop | Everything it awaits is async: httpx.AsyncClient, asyncpg, SQLAlchemy's async engine |
| `def` | In a thread pool, so blocking calls are fine | You call blocking libraries: requests, a sync database driver, a sync SDK |

The classic production bug is a blocking call inside `async def` (for example `requests.get`
or a sync ORM query): it freezes every request on that worker. Either make it async, move it
to `def`, or wrap it in `await asyncio.to_thread(...)`. CPU-heavy work belongs in a task queue
(Celery, Dramatiq, arq) or a process pool, not in a request.

## Errors, settings and lifespan

```python
from contextlib import asynccontextmanager
from fastapi import Request
from fastapi.responses import JSONResponse
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="ORDERS_", env_file=".env")
    database_url: str
    payment_timeout: float = 3.0

settings = Settings()        # read and validated from the environment at start-up

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.http = httpx.AsyncClient(timeout=settings.payment_timeout)   # one pool for the app
    yield
    await app.state.http.aclose()

app = FastAPI(lifespan=lifespan)

@app.exception_handler(PaymentError)
async def payment_error(request: Request, error: PaymentError) -> JSONResponse:
    return JSONResponse(status_code=402, content={"detail": str(error)})
```

Translate domain exceptions into HTTP responses in one place, and let unexpected exceptions
become a 500 that is logged with its traceback, never shown to the client.

## Testing

```python
from fastapi.testclient import TestClient

def fake_user() -> User:
    return User(id=1, name="ada")

def test_create_order():
    app.dependency_overrides[current_user] = fake_user
    client = TestClient(app)
    response = client.post("/orders", json={"sku": "TEA-01", "quantity": 2})
    assert response.status_code == 201
    assert response.json()["sku"] == "TEA-01"
    assert client.post("/orders", json={"sku": "", "quantity": 0}).status_code == 422
    app.dependency_overrides.clear()
```

`dependency_overrides` swaps any dependency (the user, the session, an external client) for a
fake. For async tests that call the app directly, use `httpx.AsyncClient` with an
`ASGITransport`.

## The wider landscape

**Django** remains the batteries-included choice (ORM, admin, migrations, auth) for
database-centred applications, with Django REST Framework or Django Ninja for APIs.
**Flask** is a small WSGI framework. **Litestar** is an ASGI alternative close to FastAPI. For
the database, **SQLAlchemy 2.0** (sync or async, with typed `Mapped[...]` models) and **Alembic**
migrations are the standard pair; SQLModel combines SQLAlchemy and Pydantic models.

## Summary

- ASGI servers (Uvicorn) run async frameworks such as FastAPI.
- Type hints and Pydantic models drive validation (422), serialisation and OpenAPI; separate
  input and output models.
- `Depends` for sessions, users and clients, with `yield` for cleanup; `dependency_overrides`
  in tests.
- `async def` only with async libraries; `def` for blocking ones; heavy work goes to a queue.
- Handle domain errors in exception handlers; configure with pydantic-settings; manage shared
  clients in `lifespan`.

## Further reading

- FastAPI tutorial, "First Steps": <https://fastapi.tiangolo.com/tutorial/first-steps/>
- FastAPI, "Dependencies": <https://fastapi.tiangolo.com/tutorial/dependencies/>
- FastAPI, "Concurrency and async / await": <https://fastapi.tiangolo.com/async/>
- Pydantic, "Settings Management": <https://docs.pydantic.dev/latest/concepts/pydantic_settings/>
- The ASGI specification: <https://asgi.readthedocs.io/en/latest/>
- roadmap.sh, Python topic "FastAPI": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/fast-api@XeQSmvAsGSTi8dd7QVHxn.md>
