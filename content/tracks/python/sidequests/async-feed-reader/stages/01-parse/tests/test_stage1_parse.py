"""Stage 1 contract: parse_feed reads RSS 2.0 items."""

from datetime import UTC, datetime, timedelta, timezone

import pytest
from feedkit import rss, rss_item

from feeds import FeedError, parse_feed


def test_items_come_in_document_order():
    items = parse_feed(rss(rss_item("One", "https://a/1"), rss_item("Two", "https://a/2")))
    assert [(i.title, i.link) for i in items] == [("One", "https://a/1"), ("Two", "https://a/2")]


def test_text_is_stripped():
    [item] = parse_feed(rss(rss_item("  Spaced  ", "\n https://a/x \n")))
    assert (item.title, item.link) == ("Spaced", "https://a/x")


def test_pub_date_becomes_an_aware_datetime():
    [item] = parse_feed(rss(rss_item("T", "https://a", "Mon, 16 Nov 2026 09:30:00 +0000")))
    assert item.published == datetime(2026, 11, 16, 9, 30, tzinfo=UTC)
    assert item.published.tzinfo is not None


def test_other_offsets_are_kept():
    [item] = parse_feed(rss(rss_item("T", "https://a", "Sun, 15 Nov 2026 22:00:00 -0300")))
    assert item.published == datetime(2026, 11, 15, 22, 0, tzinfo=timezone(timedelta(hours=-3)))
    assert item.published == datetime(2026, 11, 16, 1, 0, tzinfo=UTC)


def test_a_missing_or_bad_date_is_none():
    items = parse_feed(rss(rss_item("A", "https://a/1"), rss_item("B", "https://a/2", "yesterday-ish")))
    assert [i.published for i in items] == [None, None]


def test_a_missing_title_is_empty_and_an_item_without_link_is_skipped():
    items = parse_feed(rss(rss_item(None, "https://a/1"), rss_item("No link", None)))
    assert [(i.title, i.link) for i in items] == [("", "https://a/1")]


def test_items_are_frozen_dataclasses():
    [item] = parse_feed(rss(rss_item("T", "https://a")))
    with pytest.raises(AttributeError):
        item.title = "changed"


@pytest.mark.parametrize("text", ["", "not xml at all", "<html><body>hi</body></html>", "<rss><channel>"])
def test_anything_else_is_a_feed_error(text):
    with pytest.raises(FeedError):
        parse_feed(text)


def test_an_empty_channel_has_no_items():
    assert parse_feed(rss()) == []
