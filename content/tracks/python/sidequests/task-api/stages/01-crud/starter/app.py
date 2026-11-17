"""A to-do list API. See README.md."""

from fastapi import FastAPI


def create_app(db_path: str) -> FastAPI:
    app = FastAPI(title="Tasks")
    # todo: the routes
    return app
