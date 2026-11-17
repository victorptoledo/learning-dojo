"""Stage 3 contract: the tasks live in SQLite, and the list filters in SQL."""

import sqlite3

from fastapi.testclient import TestClient

from app import create_app


def test_a_second_app_on_the_same_file_sees_the_tasks(db_path, client):
    client.post("/tasks", json={"title": "Persist me", "done": True})
    with TestClient(create_app(db_path)) as again:
        assert again.get("/tasks/1").json() == {"id": 1, "title": "Persist me", "done": True}
        again.post("/tasks", json={"title": "next"})
        assert again.get("/tasks").json()["total"] == 2


def test_other_tools_can_read_the_table(db_path, client):
    client.post("/tasks", json={"title": "Buy milk"})
    client.post("/tasks", json={"title": "Call mum", "done": True})
    client.patch("/tasks/1", json={"title": "Buy oat milk"})
    with sqlite3.connect(db_path) as connection:
        rows = connection.execute("SELECT id, title, done FROM tasks ORDER BY id").fetchall()
    assert rows == [(1, "Buy oat milk", 0), (2, "Call mum", 1)]


def test_deletes_reach_the_file(db_path, client):
    client.post("/tasks", json={"title": "gone"})
    client.delete("/tasks/1")
    with sqlite3.connect(db_path) as connection:
        assert connection.execute("SELECT count(*) FROM tasks").fetchone() == (0,)


def test_ids_keep_going_after_a_restart(db_path, client):
    client.post("/tasks", json={"title": "a"})
    client.post("/tasks", json={"title": "b"})
    with TestClient(create_app(db_path)) as again:
        assert again.post("/tasks", json={"title": "c"}).json()["id"] == 3


def seed(client):
    for title, done in [("Buy milk", True), ("Buy bread", False), ("Call MILKMAN", False), ("Read", True)]:
        client.post("/tasks", json={"title": title, "done": done})


def titles(response):
    return [task["title"] for task in response.json()["items"]]


def test_filter_by_done(client):
    seed(client)
    assert titles(client.get("/tasks", params={"done": "true"})) == ["Buy milk", "Read"]
    response = client.get("/tasks", params={"done": "false"})
    assert titles(response) == ["Buy bread", "Call MILKMAN"]
    assert response.json()["total"] == 2


def test_search_ignores_case_and_combines_with_done(client):
    seed(client)
    assert titles(client.get("/tasks", params={"q": "milk"})) == ["Buy milk", "Call MILKMAN"]
    assert titles(client.get("/tasks", params={"q": "milk", "done": "false"})) == ["Call MILKMAN"]


def test_search_values_are_not_sql(client):
    seed(client)
    response = client.get("/tasks", params={"q": "x' OR '1'='1"})
    assert response.status_code == 200
    assert response.json()["total"] == 0
    assert titles(client.get("/tasks", params={"q": "%"})) == [], "% is a character, not a wildcard"
