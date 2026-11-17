# Sidequest: Task API

Build a small HTTP API for a to-do list with **FastAPI** and **Pydantic**, storing the tasks
in SQLite through the standard `sqlite3` module and SQL you write yourself.

The fixed piece is a factory in `app.py`:

```python
def create_app(db_path: str) -> FastAPI: ...
```

Every app it returns keeps its data in the SQLite file `db_path`. The contract tests build an
app on a temporary file and talk to it with FastAPI's `TestClient`, so they check what an HTTP
client sees: status codes, JSON bodies, and (from stage 3) what is in the database. Models,
routers, a repository class, dependency injection with `Depends`... the structure is yours.

FastAPI, Pydantic and httpx are installed in the dojo's image. To run the app yourself,
install them in a virtual environment (`uv add fastapi uvicorn`) and start
`uvicorn app:app --factory` after adding a module-level factory call, or simply write your
own tests with `TestClient` in `tests/`.
