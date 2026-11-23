"""A fresh bucket in moto (an in-memory S3) for every test, and a log of the requests sent."""

import boto3
import pytest
from moto import mock_aws

BUCKET = "dojo-sitebucket"


class Calls:
    """Every S3 operation the client sends, in order, with its parameters."""

    def __init__(self):
        self.log: list[tuple[str, dict]] = []

    def __call__(self, model, params, **_):
        self.log.append((model.name, params))

    def names(self) -> list[str]:
        return [name for name, _ in self.log]

    def of(self, name: str) -> list[dict]:
        return [params for found, params in self.log if found == name]

    def clear(self):
        self.log.clear()


@pytest.fixture
def s3():
    with mock_aws():
        client = boto3.client("s3", region_name="eu-west-1")
        client.create_bucket(Bucket=BUCKET, CreateBucketConfiguration={"LocationConstraint": "eu-west-1"})
        yield client


@pytest.fixture
def calls(s3):
    recorder = Calls()
    s3.meta.events.register("provide-client-params.s3.*", recorder)
    return recorder


@pytest.fixture
def site(tmp_path):
    """A small site; returns a function that writes more files into it."""
    root = tmp_path / "site"

    def write(files: dict[str, str | bytes]):
        for key, body in files.items():
            path = root / key
            path.parent.mkdir(parents=True, exist_ok=True)
            if isinstance(body, bytes):
                path.write_bytes(body)
            else:
                path.write_text(body, encoding="utf-8")
        return root

    write({
        "index.html": "<h1>Studio</h1>",
        "about.html": "<h1>About</h1>",
        "404.html": "<h1>Not here</h1>",
        "assets/app.3f9c.css": "body { margin: 0 }",
        "assets/app.81aa.js": "console.log('hi')",
        "img/logo.SVG": "<svg/>",
        "robots.txt": "User-agent: *",
    })
    write.root = root
    return write
