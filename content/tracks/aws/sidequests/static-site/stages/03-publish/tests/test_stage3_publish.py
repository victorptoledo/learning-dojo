import hashlib

import pytest

from conftest import BUCKET


def run(s3, folder, **options):
    from publish import publish

    return publish(s3, BUCKET, folder, **options)


def keys(s3):
    found = []
    for page in s3.get_paginator("list_objects_v2").paginate(Bucket=BUCKET):
        found.extend(item["Key"] for item in page.get("Contents", []))
    return sorted(found)


def test_the_first_publish_uploads_every_file_and_skips_hidden_ones(s3, site):
    site({".env": "SECRET=1", ".git/config": "[core]", "drafts/.wip.html": "x"})
    answer = run(s3, site.root)
    expected = ["404.html", "about.html", "assets/app.3f9c.css", "assets/app.81aa.js",
                "img/logo.SVG", "index.html", "robots.txt"]
    assert answer == {"uploaded": expected, "unchanged": [], "deleted": [], "invalidate": []}, (
        f"first publish of a new site: {answer}")
    assert keys(s3) == expected, "the bucket must hold exactly the site's files, hidden ones skipped"


def test_the_folder_may_be_given_as_text(s3, site):
    assert len(run(s3, str(site.root))["uploaded"]) == 7, "folder may be a str as well as a Path"


@pytest.mark.parametrize("key,content_type,cache", [
    ("index.html", "text/html; charset=utf-8", "no-cache"),
    ("404.html", "text/html; charset=utf-8", "no-cache"),
    ("assets/app.3f9c.css", "text/css; charset=utf-8", "public, max-age=31536000, immutable"),
    ("assets/app.81aa.js", "text/javascript; charset=utf-8", "public, max-age=31536000, immutable"),
    ("img/logo.SVG", "image/svg+xml", "public, max-age=3600"),
    ("robots.txt", "text/plain; charset=utf-8", "public, max-age=3600"),
])
def test_each_object_gets_its_content_type_and_cache_header(s3, site, key, content_type, cache):
    run(s3, site.root)
    head = s3.head_object(Bucket=BUCKET, Key=key)
    assert head["ContentType"] == content_type, f"{key}: ContentType {head['ContentType']!r}, expected {content_type!r}"
    assert head.get("CacheControl") == cache, f"{key}: CacheControl {head.get('CacheControl')!r}, expected {cache!r}"


def test_unknown_extensions_are_octet_streams(s3, site):
    site({"files/brochure.pdf2": b"%PDF", "assets/font.WOFF2": b"wOF2", "data.json": "{}"})
    run(s3, site.root)
    found = {key: s3.head_object(Bucket=BUCKET, Key=key)["ContentType"]
             for key in ("files/brochure.pdf2", "assets/font.WOFF2", "data.json")}
    assert found == {"files/brochure.pdf2": "application/octet-stream", "assets/font.WOFF2": "font/woff2",
                     "data.json": "application/json"}, f"content types: {found}"


def test_the_bodies_are_the_files_bytes(s3, site):
    site({"img/photo.png": bytes(range(256)) * 40})
    run(s3, site.root)
    body = s3.get_object(Bucket=BUCKET, Key="img/photo.png")["Body"].read()
    assert body == bytes(range(256)) * 40, "upload the file's bytes unchanged"


def test_pages_are_uploaded_after_everything_else(s3, site, calls):
    run(s3, site.root)
    order = [params["Key"] for params in calls.of("PutObject")]
    pages = [i for i, key in enumerate(order) if key.endswith(".html")]
    others = [i for i, key in enumerate(order) if not key.endswith(".html")]
    assert pages and others and max(others) < min(pages), f"upload the pages last; the order was {order}"


def test_a_second_publish_sends_nothing_and_reads_one_listing(s3, site, calls):
    run(s3, site.root)
    calls.clear()
    answer = run(s3, site.root)
    assert answer["uploaded"] == [] and answer["deleted"] == [] and answer["invalidate"] == [], (
        f"nothing changed, nothing to do: {answer}")
    assert len(answer["unchanged"]) == 7, "every file is unchanged"
    names = calls.names()
    assert "PutObject" not in names, "do not upload files whose MD5 equals the object's ETag"
    assert "HeadObject" not in names and "GetObject" not in names, (
        "read the ETags from the listing, not with one request per file")
    assert names.count("ListObjectsV2") == 1, f"one listing page is enough for 7 objects, sent {names}"


def test_only_changed_files_are_sent_and_overwrites_are_invalidated(s3, site, calls):
    run(s3, site.root)
    calls.clear()
    site({"index.html": "<h1>Studio 2</h1>", "assets/app.77de.css": "body { margin: 1px }"})
    answer = run(s3, site.root)
    assert answer["uploaded"] == ["assets/app.77de.css", "index.html"], f"uploaded: {answer['uploaded']}"
    assert answer["invalidate"] == ["/", "/index.html"], (
        f"an overwritten index.html invalidates /index.html and /, a new asset nothing: {answer['invalidate']}")
    sent = sorted(params["Key"] for params in calls.of("PutObject"))
    assert sent == ["assets/app.77de.css", "index.html"], f"only the changed files are sent, sent {sent}"


def test_files_gone_from_the_folder_are_deleted_and_invalidated(s3, site):
    run(s3, site.root)
    (site.root / "about.html").unlink()
    (site.root / "assets" / "app.81aa.js").unlink()
    answer = run(s3, site.root)
    assert answer["deleted"] == ["about.html", "assets/app.81aa.js"], f"deleted: {answer['deleted']}"
    assert answer["invalidate"] == ["/about.html", "/assets/app.81aa.js"], f"invalidate: {answer['invalidate']}"
    assert "about.html" not in keys(s3), "about.html must be gone from the bucket"


def test_thousands_of_stale_keys_are_listed_in_pages_and_deleted_in_batches(s3, site, calls):
    for number in range(2300):
        s3.put_object(Bucket=BUCKET, Key=f"old/{number:05}.html", Body=b"old")
    calls.clear()
    answer = run(s3, site.root)
    names = calls.names()
    batches = [len(params["Delete"]["Objects"]) for params in calls.of("DeleteObjects")]
    assert len(answer["deleted"]) == 2300, f"all 2,300 stale keys must be deleted, got {len(answer['deleted'])}"
    assert names.count("ListObjectsV2") == 3, "follow every page of the listing (1,000 keys per page)"
    assert batches and max(batches) <= 1000 and sum(batches) == 2300, (
        f"delete_objects takes at most 1,000 keys per call; batches were {batches}")
    assert names.index("DeleteObjects") > max(
        i for i, name in enumerate(names) if name == "PutObject"), "delete after uploading"
    assert keys(s3) == sorted(answer["uploaded"]), "only the site's files are left"


def test_failed_deletions_raise(s3, site, monkeypatch):
    run(s3, site.root)
    (site.root / "robots.txt").unlink()

    def refuse(**request):
        keys = [item["Key"] for item in request["Delete"]["Objects"]]
        return {"Errors": [{"Key": key, "Code": "AccessDenied", "Message": "Access Denied"} for key in keys]}

    monkeypatch.setattr(s3, "delete_objects", refuse)
    with pytest.raises(Exception, match="robots.txt"):
        run(s3, site.root)


def test_a_dry_run_reports_the_same_and_changes_nothing(s3, site, calls):
    run(s3, site.root)
    site({"about.html": "<h1>About us</h1>"})
    (site.root / "robots.txt").unlink()
    before = {key: s3.head_object(Bucket=BUCKET, Key=key)["ETag"] for key in keys(s3)}
    calls.clear()
    answer = run(s3, site.root, dry_run=True)
    assert answer["uploaded"] == ["about.html"] and answer["deleted"] == ["robots.txt"], f"dry run: {answer}"
    assert answer["invalidate"] == ["/about.html", "/robots.txt"], f"dry run: {answer}"
    assert not {"PutObject", "DeleteObjects", "DeleteObject"} & set(calls.names()), (
        f"a dry run sends no writes, sent {calls.names()}")
    after = {key: s3.head_object(Bucket=BUCKET, Key=key)["ETag"] for key in keys(s3)}
    assert after == before, "a dry run must leave the bucket as it was"


def test_unchanged_means_the_same_md5(s3, site):
    body = "<h1>Studio</h1>"
    s3.put_object(Bucket=BUCKET, Key="index.html", Body=body.encode())
    s3.put_object(Bucket=BUCKET, Key="about.html", Body=b"<h1>Old about</h1>")
    answer = run(s3, site.root)
    assert "index.html" in answer["unchanged"], (
        f"index.html has the MD5 {hashlib.md5(body.encode()).hexdigest()} already in the bucket")
    assert "about.html" in answer["uploaded"] and "/about.html" in answer["invalidate"], (
        "about.html differs: upload it and invalidate it")
