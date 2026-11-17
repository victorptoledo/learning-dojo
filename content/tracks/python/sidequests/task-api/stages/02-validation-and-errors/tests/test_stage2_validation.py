"""Stage 2 contract: Pydantic validation, PATCH and DELETE."""

import pytest


@pytest.mark.parametrize(
    "body",
    [
        {},
        {"title": ""},
        {"title": "   "},
        {"title": "x" * 201},
        {"title": "ok", "done": "yes"},
        {"title": "ok", "done": 1},
        {"title": "ok", "priority": "high"},
        {"title": 42},
    ],
)
def test_bad_bodies_are_refused_with_422(client, body):
    response = client.post("/tasks", json=body)
    assert response.status_code == 422, response.text
    assert isinstance(response.json()["detail"], list)
    assert client.get("/tasks").json()["total"] == 0


def test_titles_are_stripped_and_200_characters_is_fine(client):
    assert client.post("/tasks", json={"title": "  Buy milk \n"}).json()["title"] == "Buy milk"
    assert client.post("/tasks", json={"title": "x" * 200}).status_code == 201


def test_patch_changes_only_what_it_is_given(client):
    client.post("/tasks", json={"title": "Write report"})
    response = client.patch("/tasks/1", json={"done": True})
    assert response.status_code == 200
    assert response.json() == {"id": 1, "title": "Write report", "done": True}
    response = client.patch("/tasks/1", json={"title": " Send report "})
    assert response.json() == {"id": 1, "title": "Send report", "done": True}
    assert client.get("/tasks/1").json() == {"id": 1, "title": "Send report", "done": True}


def test_an_empty_patch_changes_nothing(client):
    client.post("/tasks", json={"title": "Keep me"})
    assert client.patch("/tasks/1", json={}).json() == {"id": 1, "title": "Keep me", "done": False}


@pytest.mark.parametrize("body", [{"title": ""}, {"done": "no"}, {"colour": "red"}])
def test_a_bad_patch_is_refused(client, body):
    client.post("/tasks", json={"title": "Stay"})
    assert client.patch("/tasks/1", json=body).status_code == 422
    assert client.get("/tasks/1").json()["title"] == "Stay"


def test_delete_removes_the_task(client):
    client.post("/tasks", json={"title": "a"})
    client.post("/tasks", json={"title": "b"})
    response = client.delete("/tasks/1")
    assert response.status_code == 204
    assert response.content == b""
    assert client.get("/tasks/1").status_code == 404
    assert [t["id"] for t in client.get("/tasks").json()["items"]] == [2]


@pytest.mark.parametrize("method", ["patch", "delete"])
def test_missing_tasks_are_404(client, method):
    kwargs = {"json": {"done": True}} if method == "patch" else {}
    response = getattr(client, method)("/tasks/9", **kwargs)
    assert response.status_code == 404
    assert response.json() == {"detail": "task 9 not found"}


def test_a_non_numeric_id_is_422(client):
    assert client.get("/tasks/abc").status_code == 422
