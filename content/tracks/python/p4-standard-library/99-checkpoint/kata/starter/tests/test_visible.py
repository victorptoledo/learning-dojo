import json
from datetime import UTC, datetime

from agenda import import_inbox, weekly_counts, write_agenda


def test_one_export_becomes_one_agenda(tmp_path):
    inbox = tmp_path / "inbox"
    inbox.mkdir()
    (inbox / "team.csv").write_text(
        "title,date,start,duration,zone\n"
        "Planning,2026-11-26,09:30,1h 30m,Europe/Lisbon\n"
        "Retro,2026-11-26,25:00,1h,Europe/Lisbon\n",
        encoding="utf-8",
    )
    result = import_inbox(inbox)
    (event,) = result.events
    assert event.start == datetime(2026, 11, 26, 9, 30, tzinfo=UTC)
    assert event.end == datetime(2026, 11, 26, 11, 0, tzinfo=UTC)
    assert [(r.file, r.line, r.reason) for r in result.rejected] == [("team.csv", 3, "bad time")]
    assert weekly_counts(result.events, "Europe/Lisbon") == {(2026, 48): 1}

    write_agenda(result, tmp_path / "agenda.json")
    saved = json.loads((tmp_path / "agenda.json").read_text(encoding="utf-8"))
    assert saved["events"][0]["start"] == "2026-11-26T09:30:00+00:00"
