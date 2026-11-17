"""A to-do list API: FastAPI in front, SQLite behind, SQL written by hand."""

import sqlite3
from contextlib import closing

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

SCHEMA = "CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY, title TEXT NOT NULL, done INTEGER NOT NULL)"

class NewTask(BaseModel):
    title: str
    done: bool = False

class Task(BaseModel):
    id: int
    title: str
    done: bool

class TaskList(BaseModel):
    items: list[Task]
    total: int

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
    ) -> TaskList:
        where, values = [], []
        clause = f" WHERE {' AND '.join(where)}" if where else ""
        order, page = "id", ""
        with closing(connect()) as connection:
            (total,) = connection.execute(f"SELECT count(*) FROM tasks{clause}", values).fetchone()
            rows = connection.execute(f"SELECT id, title, done FROM tasks{clause} ORDER BY {order}{page}", values)
            items = [Task(id=row[0], title=row[1], done=bool(row[2])) for row in rows]
        return TaskList(items=items, total=total)

    return app
