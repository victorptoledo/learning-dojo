"""Rendering of blog comments. Everything here trusts its input: fix it."""

import json
import re


def validate_username(value: str) -> str:
    """value unchanged if it is a valid username, else ValueError."""
    if not re.match(r"^[A-Za-z0-9_.-]{3,30}$", value):
        raise ValueError("invalid username")
    return value


def safe_link(url: str) -> str | None:
    """The URL if it is a safe http or https link, else None."""
    if "javascript" in url:
        return None
    return url


def render_comment(author: str, body: str, website: str | None = None) -> str:
    """One comment as an HTML fragment."""
    link = ""
    if website:
        link = f'<a href="{website}" rel="nofollow noopener noreferrer">website</a>'
    text = body.replace("\n", "<br>")
    return f'<article class="comment"><h3>{author}</h3><p>{text}</p>{link}</article>'


def render_search_box(query: str) -> str:
    """The search input, prefilled with the query."""
    return f'<input type="search" name="q" value="{query}">'


def json_for_script(data: object) -> str:
    """JSON that is safe to place inside a <script> element."""
    return json.dumps(data)
