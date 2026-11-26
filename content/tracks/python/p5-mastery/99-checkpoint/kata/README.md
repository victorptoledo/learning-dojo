# Production-Ready Orders Service

The orders API works on a laptop. Before it goes live it needs what every Python service in
production needs: configuration from the environment that fails clearly and never leaks the
database password, logs as JSON lines with a request id, a database opened once and closed on
shutdown, input validated at the edge, internal fields kept inside, and errors that are logged
in full but shown to the client in one line. Write `service.py`. `Product`, `OrderRecord` and
the `OrderRepository` protocol are given; the tests bring their own repository.

## Settings

`load_settings(env)` reads a mapping such as `os.environ` and returns a `Settings` (a Pydantic
model):

| Variable | Field | Rule |
| --- | --- | --- |
| `APP_ENV` | `app_env` | Required: `dev`, `test` or `prod` |
| `DATABASE_URL` | `database_url` | Required, a `SecretStr`, must start with `postgresql://` |
| `MAX_PAGE_SIZE` | `max_page_size` | Whole number from 1 to 500, default 50 |
| `LOG_LEVEL` | `log_level` | `DEBUG`, `INFO`, `WARNING` or `ERROR`, default `INFO` |

- `repr()` and `str()` of the settings never show the URL; `get_secret_value()` does.
- Anything wrong raises `SettingsError` (a `ValueError`) naming **every** bad variable by its
  environment name, for example `invalid settings: APP_ENV, DATABASE_URL`. The message must not
  contain the values (they can be secrets), and neither may a chained cause: Pydantic's own
  message repeats the input.

## Logging

- `JsonFormatter` turns a record into **one line** of JSON with `time` (the record's time as
  ISO 8601 in UTC, `+00:00`), `level`, `logger`, `message` (with its `%s` arguments applied),
  `request_id` when the record has one, and `exc` (the formatted traceback) when it has
  `exc_info`. Non-ASCII text stays readable.
- `configure_logging(settings, stream=None)` sends the `service` logger and its children
  (`service.access`, ...) to `stream` (default `sys.stdout`) through that formatter, at
  `settings.log_level`. Calling it again **replaces** the handler, so lines are never doubled,
  and the records do not also propagate to the root logger.

## The app: `create_app(settings, repo_factory, *, clock)`

| Behaviour | |
| --- | --- |
| Lifespan | `repo_factory()` is called once at startup and `open()`ed; `close()` runs at shutdown. Every request uses that one repository |
| `GET /health` | `200 {"status": "ok"}` when `ping()` is true; `503 {"status": "unavailable"}` when it is false **or raises** |
| `POST /orders` | Body `{"sku", "quantity"}`: `sku` matches `^[A-Z0-9-]{3,20}$`, `quantity` 1 to 100, **no other fields**. Invalid bodies get FastAPI's own 422. An unknown SKU is `422 {"detail": "unknown sku"}`. Otherwise `201` with the order |
| The order | `{"id", "sku", "quantity", "unit_cents", "total_cents", "created_at"}`; `created_at` comes from `clock()` and the cost price **never** appears |
| `GET /orders/{order_id}` | The order, or `404 {"detail": "order not found"}` |
| `GET /orders?limit=&offset=` | `{"items", "limit", "offset"}`. `limit` defaults to 20 (or `max_page_size` if that is smaller), must be 1 to `max_page_size`; `offset` at least 0; otherwise 422 |
| Request id | Use the incoming `X-Request-ID` if it is 1 to 64 letters, digits or `-`; otherwise make a new random one (at least 16 characters). Send it back in `X-Request-ID` on every response |
| Access log | After each request, an `INFO` line on `service.access`: `"GET /orders/7 404"` (method, path, status), with the request id |
| Errors | An unexpected exception becomes `500 {"detail": "internal error", "request_id": ...}`: no traceback, no exception text. It is logged once at `ERROR` on `service`, with its traceback and the request id, and the service keeps serving |

The repository is synchronous, so declare the endpoints with `def`: FastAPI runs them in a
thread pool instead of blocking the event loop.

Tools: Pydantic `BaseModel`, `Field`, `SecretStr`, `field_validator`, `ValidationError.errors()`,
`ConfigDict(extra="forbid")`, `logging.Formatter`, `formatException`, `FastAPI(lifespan=...)`,
`Depends`, `HTTPException`, `@app.middleware("http")`, `uuid.uuid4().hex`, `re.fullmatch`.

Run the visible tests with `uv run pytest`. There are 25 tests: 1 in `tests/test_visible.py`
and 24 hidden ones, plus ruff on `service.py`.
