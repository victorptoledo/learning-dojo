A task is `{"id": 1, "title": "Buy milk", "done": false}`.

| Request | Response |
|---|---|
| `POST /tasks` with `{"title": "Buy milk"}` (`done` is optional, `false` by default) | `201` and the new task; ids start at 1 and go up |
| `GET /tasks/{id}` | `200` and the task, or `404` with `{"detail": "task 7 not found"}` |
| `GET /tasks` | `200` and `{"items": [...], "total": 2}`, every task by id |

In this stage the data may live in memory: from stage 3 on it must be in the SQLite file.
Every app built by `create_app` has its own tasks.
