import json
import os
from datetime import UTC, datetime

import pytest

from agenda import Event, ImportResult, Rejected, import_inbox, weekly_counts, write_agenda

HEADER = "title,date,start,duration,zone\n"


def put(folder, name, text, encoding="utf-8"):
    (folder / name).write_bytes(text.encode(encoding))


def utc(*parts):
    return datetime(*parts, tzinfo=UTC)


def reasons(result):
    return [(r.file, r.line, r.reason) for r in result.rejected]


# ---- reading the inbox ------------------------------------------------------------------


def test_reads_every_csv_in_name_order_and_sorts_by_start(tmp_path):
    put(tmp_path, "b.csv", HEADER + "Late,2026-11-26,15:00,1h,UTC\n")
    put(tmp_path, "a.csv", HEADER + "Early,2026-11-26,09:00,30m,UTC\nMid,2026-11-26,12:00,45m,UTC\n")
    put(tmp_path, "notes.txt", HEADER + "Ignored,2026-11-26,10:00,1h,UTC\n")
    (tmp_path / "old").mkdir()
    put(tmp_path / "old", "c.csv", HEADER + "Nested,2026-11-26,10:00,1h,UTC\n")
    result = import_inbox(tmp_path)
    assert isinstance(result, ImportResult)
    assert [(e.title, e.source) for e in result.events] == [("Early", "a.csv"), ("Mid", "a.csv"), ("Late", "b.csv")]
    assert result.events[0] == Event("Early", utc(2026, 11, 26, 9), utc(2026, 11, 26, 9, 30), "UTC", "a.csv")
    assert result.rejected == ()


def test_columns_in_any_order_with_spaces_case_and_extra_columns(tmp_path):
    put(tmp_path, "x.csv", " Zone ,DURATION,notes,Title,start,date\nEurope/Lisbon,1h 30m,bring cake,Review,14:00,2026-07-01\n")
    (event,) = import_inbox(tmp_path).events
    assert event.title == "Review"
    assert event.zone == "Europe/Lisbon"
    assert event.start == utc(2026, 7, 1, 13)
    assert event.end == utc(2026, 7, 1, 14, 30)


def test_a_bom_from_excel_and_crlf_line_endings(tmp_path):
    put(tmp_path, "excel.csv", "﻿" + HEADER.replace("\n", "\r\n") + "Plan,2026-11-26,10:00,1h,UTC\r\n")
    (event,) = import_inbox(tmp_path).events
    assert event.title == "Plan"


def test_quoted_fields_with_commas_and_unicode_titles(tmp_path):
    put(tmp_path, "x.csv", HEADER + '"Lisbon, Portugal: kick-off",2026-11-26,10:00,1h,Europe/Lisbon\n'
        + "  Reunião   de   São   Paulo ,2026-11-26,10:00,1h,America/Sao_Paulo\n")
    titles = [e.title for e in import_inbox(tmp_path).events]
    assert titles == ["Lisbon, Portugal: kick-off", "Reunião de São Paulo"]


def test_titles_are_stored_in_nfc(tmp_path):
    put(tmp_path, "x.csv", HEADER + "Café,2026-11-26,10:00,1h,UTC\n")
    (event,) = import_inbox(tmp_path).events
    assert event.title == "Café"


def test_a_file_missing_a_column_is_rejected_whole(tmp_path):
    put(tmp_path, "a.csv", "title,date,start,zone\nX,2026-11-26,10:00,UTC\n")
    put(tmp_path, "b.csv", HEADER + "Y,2026-11-26,10:00,1h,UTC\n")
    put(tmp_path, "c.csv", "")
    result = import_inbox(tmp_path)
    assert [e.title for e in result.events] == ["Y"]
    assert reasons(result) == [("a.csv", 1, "missing column duration"), ("c.csv", 1, "missing column title")]


def test_a_file_that_is_not_utf8_is_rejected_whole(tmp_path):
    put(tmp_path, "legacy.csv", HEADER + "Ok,2026-11-26,10:00,1h,UTC\nReunião,2026-11-26,11:00,1h,UTC\n", encoding="cp1252")
    result = import_inbox(tmp_path)
    assert result.events == ()
    assert reasons(result) == [("legacy.csv", 0, "not utf-8")]


# ---- row errors -------------------------------------------------------------------------


def test_bad_rows_are_reported_with_their_line_and_the_rest_imported(tmp_path):
    rows = [
        "Good,2026-11-26,10:00,1h,UTC",           # line 2
        "   ,2026-11-26,10:00,1h,UTC",            # 3
        "",                                       # 4: blank, skipped
        "D,2026-02-30,10:00,1h,UTC",              # 5
        "D,26/11/2026,10:00,1h,UTC",              # 6
        "T,2026-11-26,24:00,1h,UTC",              # 7
        "T,2026-11-26,9:00,1h,UTC",               # 8
        "U,2026-11-26,10:00,1h,Mars/Olympus",     # 9
        "U,2026-11-26,10:00,1h,",                 # 10
        "R,2026-11-26,10:00,90,UTC",              # 11
        "R,2026-11-26,10:00,0h 0m,UTC",           # 12
        "R,2026-11-26,10:00,30m 1h,UTC",          # 13
        "Also good,2026-11-26,11:00,2h 15m,UTC",  # 14
    ]
    put(tmp_path, "x.csv", HEADER + "\n".join(rows) + "\n")
    result = import_inbox(tmp_path)
    assert [e.title for e in result.events] == ["Good", "Also good"]
    assert result.events[1].end == utc(2026, 11, 26, 13, 15)
    assert reasons(result) == [
        ("x.csv", 3, "empty title"),
        ("x.csv", 5, "bad date"),
        ("x.csv", 6, "bad date"),
        ("x.csv", 7, "bad time"),
        ("x.csv", 8, "bad time"),
        ("x.csv", 9, "unknown zone"),
        ("x.csv", 10, "unknown zone"),
        ("x.csv", 11, "bad duration"),
        ("x.csv", 12, "bad duration"),
        ("x.csv", 13, "bad duration"),
    ]


def test_the_first_problem_of_a_row_is_the_one_reported(tmp_path):
    put(tmp_path, "x.csv", HEADER + ",nope,nope,nope,Nowhere/City\nX,2026-11-26,nope,nope,Nowhere/City\n")
    assert [r.reason for r in import_inbox(tmp_path).rejected] == ["empty title", "bad time"]


def test_a_short_row_counts_its_missing_fields_as_empty(tmp_path):
    put(tmp_path, "x.csv", HEADER + "Only a title,2026-11-26,10:00\n")
    assert reasons(import_inbox(tmp_path)) == [("x.csv", 2, "bad duration")]


def test_ascii_digits_only(tmp_path):
    put(tmp_path, "x.csv", HEADER + "X,٢٠٢٦-11-26,10:00,1h,UTC\n")
    assert reasons(import_inbox(tmp_path)) == [("x.csv", 2, "bad date")]


# ---- time zones and daylight saving -------------------------------------------------------


def test_local_times_become_utc_instants(tmp_path):
    put(tmp_path, "x.csv", HEADER + "Sao Paulo,2026-11-26,09:00,1h,America/Sao_Paulo\nTokyo,2026-11-26,09:00,1h,Asia/Tokyo\n")
    starts = {e.title: e.start for e in import_inbox(tmp_path).events}
    assert starts == {"Tokyo": utc(2026, 11, 26, 0), "Sao Paulo": utc(2026, 11, 26, 12)}


def test_a_time_in_the_spring_gap_and_one_in_the_autumn_fold_are_rejected(tmp_path):
    put(tmp_path, "x.csv", HEADER + "Gap,2026-03-29,01:30,1h,Europe/Lisbon\nFold,2026-10-25,01:30,1h,Europe/Lisbon\n"
        + "Before,2026-03-29,00:59,1h,Europe/Lisbon\nAfter,2026-10-25,02:00,1h,Europe/Lisbon\n")
    result = import_inbox(tmp_path)
    assert reasons(result) == [("x.csv", 2, "nonexistent time"), ("x.csv", 3, "ambiguous time")]
    assert {e.title: e.start for e in result.events} == {
        "Before": utc(2026, 3, 29, 0, 59),
        "After": utc(2026, 10, 25, 2),
    }


def test_the_end_is_the_real_elapsed_time_across_a_dst_change(tmp_path):
    put(tmp_path, "x.csv", HEADER + "Night shift,2026-03-29,00:30,2h,Europe/Lisbon\n")
    (event,) = import_inbox(tmp_path).events
    assert event.start == utc(2026, 3, 29, 0, 30)
    assert event.end == utc(2026, 3, 29, 2, 30)
    assert event.start.utcoffset().total_seconds() == 0
    assert event.end.utcoffset().total_seconds() == 0


# ---- duplicates ---------------------------------------------------------------------------


def test_the_same_meeting_in_two_exports_is_kept_once(tmp_path):
    put(tmp_path, "a.csv", HEADER + "Weekly Sync,2026-11-26,10:00,1h,Europe/Lisbon\n")
    put(tmp_path, "b.csv", HEADER + "WEEKLY  sync,2026-11-26,05:00,30m,America/New_York\n"
        + "Weekly Sync,2026-11-26,11:00,1h,Europe/Lisbon\n")
    result = import_inbox(tmp_path)
    assert [(e.title, e.source, e.start.hour) for e in result.events] == [
        ("Weekly Sync", "a.csv", 10),
        ("Weekly Sync", "b.csv", 11),
    ]


def test_full_width_and_ligature_titles_are_duplicates_too(tmp_path):
    put(tmp_path, "a.csv", HEADER + "ﬁnance ２０２６,2026-11-26,10:00,1h,UTC\nFinance 2026,2026-11-26,10:00,1h,UTC\n")
    assert len(import_inbox(tmp_path).events) == 1


# ---- writing ------------------------------------------------------------------------------


def sample():
    return ImportResult(
        events=(Event("Reunião", utc(2026, 11, 26, 9), utc(2026, 11, 26, 10), "Europe/Lisbon", "a.csv"),),
        rejected=(Rejected("b.csv", 4, "bad time"),),
    )


def test_write_agenda_writes_readable_utf8_json(tmp_path):
    target = tmp_path / "agenda.json"
    write_agenda(sample(), target)
    text = target.read_text(encoding="utf-8")
    assert "Reunião" in text
    assert json.loads(text) == {
        "events": [{
            "title": "Reunião",
            "start": "2026-11-26T09:00:00+00:00",
            "end": "2026-11-26T10:00:00+00:00",
            "zone": "Europe/Lisbon",
            "source": "a.csv",
        }],
        "rejected": [{"file": "b.csv", "line": 4, "reason": "bad time"}],
    }


def test_write_agenda_replaces_atomically_from_the_same_folder(tmp_path, monkeypatch):
    target = tmp_path / "agenda.json"
    target.write_text("old", encoding="utf-8")
    calls = []
    real_replace = os.replace

    def spy(src, dst):
        calls.append((os.path.dirname(os.fspath(src)), os.fspath(dst), os.path.exists(src)))
        real_replace(src, dst)

    monkeypatch.setattr(os, "replace", spy)
    write_agenda(sample(), target)
    assert calls == [(os.fspath(tmp_path), os.fspath(target), True)]
    assert json.loads(target.read_text(encoding="utf-8"))["events"][0]["title"] == "Reunião"
    assert sorted(p.name for p in tmp_path.iterdir()) == ["agenda.json"]


def test_a_failed_write_keeps_the_old_file_and_leaves_nothing_behind(tmp_path, monkeypatch):
    target = tmp_path / "agenda.json"
    target.write_text("old", encoding="utf-8")

    def broken(src, dst):
        raise OSError("disk full")

    monkeypatch.setattr(os, "replace", broken)
    with pytest.raises(OSError):
        write_agenda(sample(), target)
    assert target.read_text(encoding="utf-8") == "old"
    assert sorted(p.name for p in tmp_path.iterdir()) == ["agenda.json"]


# ---- weekly counts ------------------------------------------------------------------------


def test_weekly_counts_use_the_iso_year_and_the_local_date():
    def at(*parts):
        return Event("e", utc(*parts), utc(*parts), "UTC", "x.csv")

    events = [
        at(2026, 12, 31, 12),   # Thursday: 2026-W53
        at(2027, 1, 1, 12),     # Friday: still 2026-W53
        at(2027, 1, 4, 12),     # Monday: 2027-W01
        at(2026, 12, 28, 2),    # Monday 02:00 UTC is Sunday 23:00 in Sao Paulo: 2026-W52 there
    ]
    assert weekly_counts(events, "UTC") == {(2026, 53): 3, (2027, 1): 1}
    assert weekly_counts(events, "America/Sao_Paulo") == {(2026, 53): 2, (2027, 1): 1, (2026, 52): 1}


def test_events_are_sorted_by_start_then_title_across_files(tmp_path):
    put(tmp_path, "a.csv", HEADER + "Zeta,2026-11-26,15:00,1h,UTC\nBeta,2026-11-26,10:00,1h,UTC\n")
    put(tmp_path, "b.csv", HEADER + "Alpha,2026-11-26,10:00,1h,UTC\nFirst,2026-11-26,08:00,1h,UTC\n")
    assert [e.title for e in import_inbox(tmp_path).events] == ["First", "Alpha", "Beta", "Zeta"]


def test_a_bad_byte_late_in_a_big_file_still_rejects_the_whole_file(tmp_path):
    good = "".join(f"Meeting {n},2026-11-26,10:00,1h,UTC\n" for n in range(2_000))
    data = (HEADER + good).encode("utf-8") + "Reunião,2026-11-26,11:00,1h,UTC\n".encode("cp1252")
    (tmp_path / "big.csv").write_bytes(data)
    put(tmp_path, "ok.csv", HEADER + "Kept,2026-11-26,09:00,1h,UTC\n")
    result = import_inbox(tmp_path)
    assert [e.title for e in result.events] == ["Kept"]
    assert reasons(result) == [("big.csv", 0, "not utf-8")]
