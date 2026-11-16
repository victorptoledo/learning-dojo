"""Rendering of blog comments, with validation and context-aware encoding."""

import html
import json
import re
from urllib.parse import urlsplit

USERNAME = re.compile(r"[A-Za-z0-9_.-]{3,30}")
SAFE_SCHEMES = {"http", "https"}
SCRIPT_ESCAPES = {
    "<": "\\u003c",
    ">": "\\u003e",
    "&": "\\u0026",
    "\u2028": "\\u2028",
    "\u2029": "\\u2029",
}


def validate_username(value: str) -> str:
    """value unchanged if it is a valid username, else ValueError."""
    if not isinstance(value, str) or USERNAME.fullmatch(value) is None:
        raise ValueError("invalid username")
    return value


def safe_link(url: str) -> str | None:
    """The URL if it is a safe http or https link, else None."""
    if any(ord(char) < 32 or ord(char) == 127 for char in url):
        return None
    candidate = url.strip()
    try:
        parts = urlsplit(candidate)
    except ValueError:
        return None
    if parts.scheme.lower() not in SAFE_SCHEMES or not parts.netloc:
        return None
    return candidate


def render_comment(author: str, body: str, website: str | None = None) -> str:
    """One comment as an HTML fragment."""
    link = ""
    url = safe_link(website) if website is not None else None
    if url is not None:
        link = f'<a href="{html.escape(url)}" rel="nofollow noopener noreferrer">website</a>'
    text = html.escape(body).replace("\n", "<br>")
    return f'<article class="comment"><h3>{html.escape(author)}</h3><p>{text}</p>{link}</article>'


def render_search_box(query: str) -> str:
    """The search input, prefilled with the query."""
    return f'<input type="search" name="q" value="{html.escape(query, quote=True)}">'


def json_for_script(data: object) -> str:
    """JSON that is safe to place inside a <script> element."""
    text = json.dumps(data)
    for char, escape in SCRIPT_ESCAPES.items():
        text = text.replace(char, escape)
    return text
