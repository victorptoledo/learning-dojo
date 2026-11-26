from release import parse_prompt_file

FILE = """---
id: ticket-triage
version: 2.3.0
model: claude-sonnet-5-5-20261001
temperature: 0
max_tokens: 400
output: category, priority
---

Classify the support ticket.
<ticket>{{input}}</ticket>
"""


def test_parse_reads_the_front_matter():
    version = parse_prompt_file(FILE)
    assert (version.id, version.version, version.output) == ("ticket-triage", (2, 3, 0), ("category", "priority"))
    assert version.label == "ticket-triage@2.3.0"


def test_parse_keeps_the_template():
    assert parse_prompt_file(FILE).template == "Classify the support ticket.\n<ticket>{{input}}</ticket>"
