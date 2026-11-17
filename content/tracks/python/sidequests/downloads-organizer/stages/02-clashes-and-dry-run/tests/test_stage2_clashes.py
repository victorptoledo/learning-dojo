"""Stage 2 contract: free names on clashes, --dry-run, and errors with exit codes."""

from pathlib import Path

import pytest

from organizer import main


def make(folder: Path, name: str, text: str = "") -> None:
    path = folder / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text or name, encoding="utf-8")


def files(folder: Path) -> list[str]:
    return sorted(p.relative_to(folder).as_posix() for p in folder.rglob("*") if p.is_file())


def test_a_clash_takes_the_first_free_number(tmp_path, capsys):
    make(tmp_path, "documents/report.pdf", "old")
    make(tmp_path, "documents/report (1).pdf", "older")
    make(tmp_path, "report.pdf", "new")
    assert main(["sort", str(tmp_path)]) == 0
    assert (tmp_path / "documents" / "report (2).pdf").read_text(encoding="utf-8") == "new"
    assert (tmp_path / "documents" / "report.pdf").read_text(encoding="utf-8") == "old"
    assert capsys.readouterr().out.strip() == "moved report.pdf -> documents/report (2).pdf"


def test_a_clash_without_extension(tmp_path):
    make(tmp_path, "other/notes")
    make(tmp_path, "notes", "second")
    main(["sort", str(tmp_path)])
    assert (tmp_path / "other" / "notes (1)").read_text(encoding="utf-8") == "second"


def test_dry_run_prints_and_changes_nothing(tmp_path, capsys):
    make(tmp_path, "cat.png")
    make(tmp_path, "documents/a.pdf")
    make(tmp_path, "a.pdf")
    assert main(["sort", str(tmp_path), "--dry-run"]) == 0
    assert capsys.readouterr().out.splitlines() == [
        "would move a.pdf -> documents/a (1).pdf",
        "would move cat.png -> images/",
    ]
    assert files(tmp_path) == ["a.pdf", "cat.png", "documents/a.pdf"]
    assert not (tmp_path / "images").exists(), "a dry run creates no folder"


@pytest.mark.parametrize("argv", [[], ["sort"], ["shuffle", "."], ["sort", ".", "--force"]])
def test_bad_arguments_are_a_usage_error(argv, capsys):
    assert main(argv) == 2
    assert capsys.readouterr().err.startswith("usage: organizer")


def test_a_missing_folder_is_an_error(tmp_path, capsys):
    missing = tmp_path / "nope"
    assert main(["sort", str(missing)]) == 1
    assert capsys.readouterr().err.strip() == f"error: not a folder: {missing}"


def test_a_file_is_not_a_folder(tmp_path, capsys):
    make(tmp_path, "plain.txt")
    target = tmp_path / "plain.txt"
    assert main(["sort", str(target)]) == 1
    assert capsys.readouterr().err.startswith("error: not a folder: ")
