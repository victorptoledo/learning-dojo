from comments import render_comment, safe_link


def test_comment_text_is_encoded():
    assert render_comment("Ada", "1 < 2") == (
        '<article class="comment"><h3>Ada</h3><p>1 &lt; 2</p></article>'
    )


def test_javascript_links_are_refused_in_any_case():
    assert safe_link("JavaScript:alert(1)") is None
