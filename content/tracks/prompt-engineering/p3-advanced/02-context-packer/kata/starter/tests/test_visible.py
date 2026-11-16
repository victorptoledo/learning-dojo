from packer import Document, render_prompt


def test_render_one_document():
    doc = Document("d1", "Travel", "Taxis are fine.", 0.9)
    assert render_prompt([(doc, doc.text)], "Taxi?", "Answer from the documents.") == (
        "<documents>\n"
        '<document index="1" id="d1" title="Travel">\nTaxis are fine.\n</document>\n'
        "</documents>\n\n"
        "Answer from the documents.\n\n"
        "<question>\nTaxi?\n</question>"
    )


def test_render_no_documents():
    assert render_prompt([], "Q", "I") == "<documents>\n</documents>\n\nI\n\n<question>\nQ\n</question>"
