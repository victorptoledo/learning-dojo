# Checkpoint 4: The Standard Library at Work

This checkpoint closes **The Standard Library at Work**. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): an agenda import. Read a folder of CSV exports
   with BOMs, Windows line endings and quoted fields; validate every row with patterns; turn
   local times into UTC instants, refusing times in a daylight saving gap or fold; drop
   duplicates by a Unicode key; and write the agenda as JSON that readers can trust.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Files, Paths, Encodings, CSV and JSON | Which encoding removes a BOM? Why does csv want `newline=""`? What makes a write atomic, and why must the temp file be in the same folder? |
| Text, Regular Expressions and Unicode | Why `fullmatch` and not `match`? Which form, NFC or NFKC, do you store, and which makes a comparison key? What does `\d` match without `re.ASCII`? |
| Dates and Times | What does `fold` choose? Is `aware + timedelta` wall time or elapsed time? Which year goes with an ISO week? |
| Text Toolkit (kata) | How did you keep the patterns linear on hostile input? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
