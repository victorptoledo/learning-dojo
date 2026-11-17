"""A to-do list API: FastAPI in front, SQLite behind, SQL written by hand."""

import sqlite3
from contextlib import closing
from typing import Annotated
from typing import Literal

from fastapi import FastAPI, HTTPException
from fastapi import Query
from fastapi import Response
from pydantic import BaseModel, ConfigDict, StrictBool, StringConstraints

SCHEMA = "CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY, title TEXT NOT NULL, done INTEGER NOT NULL)"
ORDERS = {
    "id": "id",
    "-id": "id DESC",
    "title": "lower(title), id",
    "-title": "lower(title) DESC, id DESC",
}

Title = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=200)]

class NewTask(BaseModel):
    model_config = ConfigDict(extra="forbid")
    title: Title
    done: StrictBool = False

class TaskChanges(BaseModel):
    model_config = ConfigDict(extra="forbid")
    title: Title | None = None
    done: StrictBool | None = None

class Task(BaseModel):
    id: int
    title: str
    done: bool

class TaskList(BaseModel):
    items: list[Task]
    total: int
    next_offset: int | None

def create_app(db_path: str) -> FastAPI:
    app = FastAPI(title="Tasks")

    def connect() -> sqlite3.Connection:
        return sqlite3.connect(db_path)

    with closing(connect()) as connection, connection:
        connection.execute(SCHEMA)

    def load(task_id: int) -> Task:
        with closing(connect()) as connection:
            row = connection.execute("SELECT id, title, done FROM tasks WHERE id = ?", (task_id,)).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail=f"task {task_id} not found")
        return Task(id=row[0], title=row[1], done=bool(row[2]))

    @app.post("/tasks", status_code=201)
    def create(task: NewTask) -> Task:
        with closing(connect()) as connection, connection:
            cursor = connection.execute("INSERT INTO tasks (title, done) VALUES (?, ?)", (task.title, int(task.done)))
        return Task(id=cursor.lastrowid, title=task.title, done=task.done)

    @app.get("/tasks/{task_id}")
    def read(task_id: int) -> Task:
        return load(task_id)

    @app.get("/tasks")
    def read_all(
        done: bool | None = None,
        q: str | None = None,
        limit: Annotated[int, Query(ge=1, le=100)] = 20,
        offset: Annotated[int, Query(ge=0)] = 0,
        sort: Literal["id", "-id", "title", "-title"] = "id",
    ) -> TaskList:
        where, values = [], []
        if done is not None:
            where.append("done = ?")
            values.append(int(done))
        if q:
            where.append("instr(lower(title), lower(?)) > 0")
            values.append(q)
        clause = f" WHERE {' AND '.join(where)}" if where else ""
        order, page = ORDERS[sort], f" LIMIT {limit} OFFSET {offset}"
        with closing(connect()) as connection:
            (total,) = connection.execute(f"SELECT count(*) FROM tasks{clause}", values).fetchone()
            rows = connection.execute(f"SELECT id, title, done FROM tasks{clause} ORDER BY {order}{page}", values)
            items = [Task(id=row[0], title=row[1], done=bool(row[2])) for row in rows]
        next_offset = offset + limit if offset + limit < total else None
        return TaskList(items=items, total=total, next_offset=next_offset)

    @app.patch("/tasks/{task_id}")
    def update(task_id: int, changes: TaskChanges) -> Task:
        task = load(task_id)
        updated = task.model_copy(update=changes.model_dump(exclude_none=True))
        with closing(connect()) as connection, connection:
            connection.execute(
                "UPDATE tasks SET title = ?, done = ? WHERE id = ?", (updated.title, int(updated.done), task_id)
            )
        return updated

    @app.delete("/tasks/{task_id}", status_code=204)
    def delete(task_id: int) -> Response:
        load(task_id)
        with closing(connect()) as connection, connection:
            connection.execute("DELETE FROM tasks WHERE id = ?", (task_id,))
        return Response(status_code=204)

    return app
