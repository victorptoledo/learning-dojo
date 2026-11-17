"""Stage 1 contract: POST, GET one and GET all."""

from fastapi.testclient import TestClient

from app import create_app


def test_post_creates_a_task_with_an_id(client):
    response = client.post("/tasks", json={"title": "Buy milk"})
    assert response.status_code == 201
    assert response.json() == {"id": 1, "title": "Buy milk", "done": False}


def test_done_can_be_given(client):
    response = client.post("/tasks", json={"title": "Already done", "done": True})
    assert response.json()["done"] is True


def test_ids_go_up(client):
    ids = [client.post("/tasks", json={"title": f"t{i}"}).json()["id"] for i in range(3)]
    assert ids == [1, 2, 3]


def test_get_returns_the_task(client):
    client.post("/tasks", json={"title": "a"})
    client.post("/tasks", json={"title": "b"})
    response = client.get("/tasks/2")
    assert response.status_code == 200
    assert response.json() == {"id": 2, "title": "b", "done": False}


def test_a_missing_task_is_404(client):
    response = client.get("/tasks/7")
    assert response.status_code == 404
    assert response.json() == {"detail": "task 7 not found"}


def test_the_list_holds_every_task_by_id(client):
    for title in ("one", "two", "three"):
        client.post("/tasks", json={"title": title})
    response = client.get("/tasks")
    assert response.status_code == 200
    body = response.json()
    assert body["total"] == 3
    assert [task["title"] for task in body["items"]] == ["one", "two", "three"]


def test_an_empty_list(client):
    body = client.get("/tasks").json()
    assert (body["items"], body["total"]) == ([], 0)


def test_apps_on_different_files_do_not_share_tasks(tmp_path):
    with TestClient(create_app(str(tmp_path / "a.db"))) as first, TestClient(create_app(str(tmp_path / "b.db"))) as second:
        first.post("/tasks", json={"title": "only in a"})
        assert second.get("/tasks").json()["total"] == 0
