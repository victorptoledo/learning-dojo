import json

import pytest

from comments import (
    json_for_script,
    render_comment,
    render_search_box,
    safe_link,
    validate_username,
)


def test_valid_usernames_are_returned_unchanged():
    for name in ["ada", "grace.hopper", "linus_t-1991", "a" * 30, "A.B"]:
        assert validate_username(name) == name


def test_invalid_usernames_are_rejected():
    bad = ["ab", "a" * 31, "ada\n", "ada ", " ada", "<script>", "ad@", "名前名前", "", "ada\x00"]
    for name in bad:
        with pytest.raises(ValueError):
            validate_username(name)
    with pytest.raises(ValueError):
        validate_username(None)


def test_safe_links_are_kept():
    assert safe_link("https://example.org/a?b=1") == "https://example.org/a?b=1"
    assert safe_link("HTTP://Example.org") == "HTTP://Example.org"
    assert safe_link("  https://example.org/  ") == "https://example.org/"


def test_dangerous_or_odd_links_are_refused():
    for url in [
        "javascript:alert(1)",
        " JaVaScRiPt:alert(1)",
        "data:text/html,<script>alert(1)</script>",
        "vbscript:msgbox(1)",
        "java\tscript:alert(1)",
        "https://exa\nmple.org",
        "//evil.example/",
        "/relative/path",
        "https:///no-host",
        "ftp://example.org/file",
        "file:///etc/passwd",
        "https://[::1",
        "",
    ]:
        assert safe_link(url) is None, url


def test_comment_encodes_author_and_body():
    html_out = render_comment('<img src=x onerror="alert(1)">', "<script>alert('x')</script>")
    assert html_out == (
        '<article class="comment"><h3>&lt;img src=x onerror=&quot;alert(1)&quot;&gt;</h3>'
        "<p>&lt;script&gt;alert(&#x27;x&#x27;)&lt;/script&gt;</p></article>"
    )


def test_newlines_become_br_after_encoding():
    assert render_comment("Ada", "line 1\nline <2>\n") == (
        '<article class="comment"><h3>Ada</h3><p>line 1<br>line &lt;2&gt;<br></p></article>'
    )


def test_comment_with_a_safe_link_encodes_the_url():
    assert render_comment("Ada", "hi", "https://example.org/?a=1&b=\"2\"") == (
        '<article class="comment"><h3>Ada</h3><p>hi</p>'
        '<a href="https://example.org/?a=1&amp;b=&quot;2&quot;" '
        'rel="nofollow noopener noreferrer">website</a></article>'
    )


def test_comment_drops_unsafe_links():
    expected = '<article class="comment"><h3>Ada</h3><p>hi</p></article>'
    assert render_comment("Ada", "hi", "javascript:alert(document.cookie)") == expected
    assert render_comment("Ada", "hi", '" onmouseover="alert(1)') == expected
    assert render_comment("Ada", "hi", None) == expected


def test_search_box_cannot_break_out_of_the_attribute():
    assert render_search_box('"><script>alert(1)</script>') == (
        '<input type="search" name="q" '
        'value="&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;">'
    )
    assert render_search_box("tom & jerry's") == (
        '<input type="search" name="q" value="tom &amp; jerry&#x27;s">'
    )


def test_json_for_script():
    data = {"name": "</script><script>alert(1)</script>", "note": "a & b", "sep": "x\u2028y\u2029z"}
    text = json_for_script(data)
    assert "<" not in text and ">" not in text and "&" not in text
    assert "\u2028" not in text and "\u2029" not in text
    assert "\\u003c/script\\u003e" in text
    assert json.loads(text) == data
    assert json_for_script([1, "two", None]) == '[1, "two", null]'
