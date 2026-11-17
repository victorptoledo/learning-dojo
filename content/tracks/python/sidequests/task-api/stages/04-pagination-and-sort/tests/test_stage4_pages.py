"""Stage 4 contract: limit, offset, next_offset and sort."""

import pytest


def seed(client, count):
    for i in range(1, count + 1):
        client.post("/tasks", json={"title": f"task {i:02}", "done": i % 2 == 0})


def ids(response):
    return [task["id"] for task in response.json()["items"]]


def test_the_default_page_holds_twenty(client):
    seed(client, 25)
    body = client.get("/tasks").json()
    assert [t["id"] for t in body["items"]] == list(range(1, 21))
    assert (body["total"], body["next_offset"]) == (25, 20)


def test_limit_and_offset_walk_the_pages(client):
    seed(client, 7)
    first = client.get("/tasks", params={"limit": 3}).json()
    assert ([t["id"] for t in first["items"]], first["next_offset"]) == ([1, 2, 3], 3)
    last = client.get("/tasks", params={"limit": 3, "offset": 6}).json()
    assert ([t["id"] for t in last["items"]], last["total"], last["next_offset"]) == ([7], 7, None)
    beyond = client.get("/tasks", params={"offset": 50}).json()
    assert (beyond["items"], beyond["total"], beyond["next_offset"]) == ([], 7, None)


@pytest.mark.parametrize("params", [{"limit": 0}, {"limit": 101}, {"offset": -1}, {"sort": "done"}, {"limit": "many"}])
def test_bad_paging_or_sorting_is_422(client, params):
    assert client.get("/tasks", params=params).status_code == 422


def test_sorting(client):
    for title in ("banana", "Apple", "cherry", "apple"):
        client.post("/tasks", json={"title": title})
    assert ids(client.get("/tasks", params={"sort": "-id"})) == [4, 3, 2, 1]
    assert ids(client.get("/tasks", params={"sort": "title"})) == [2, 4, 1, 3]
    assert ids(client.get("/tasks", params={"sort": "-title"})) == [3, 1, 4, 2]


def test_filters_sort_and_pages_combine(client):
    seed(client, 10)
    body = client.get("/tasks", params={"done": "true", "sort": "-id", "limit": 2, "offset": 1}).json()
    assert [t["id"] for t in body["items"]] == [8, 6]
    assert (body["total"], body["next_offset"]) == (5, 3)
