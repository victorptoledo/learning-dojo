from pathlib import Path

from pipeline import parse

SAMPLE = Path(__file__).parent.parent / "sample.log"


def test_parses_the_sample():
    with SAMPLE.open(encoding="utf-8") as lines:
        entries = list(parse(lines))
    assert [entry.level for entry in entries] == ["INFO", "ERROR", "DEBUG", "WARNING", "CRITICAL", "INFO"]
