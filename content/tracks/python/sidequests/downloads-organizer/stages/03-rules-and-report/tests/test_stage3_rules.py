"""Stage 3 contract: categories from .organizer.toml, and the report command."""

from pathlib import Path

from organizer import main


def make(folder: Path, name: str, size: int = 10) -> None:
    path = folder / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"x" * size)


def files(folder: Path) -> list[str]:
    return sorted(
        p.relative_to(folder).as_posix() for p in folder.rglob("*") if p.is_file() and not p.name.startswith(".")
    )


RULES = """
[categories]
photos = ["jpg", "PNG", "heic"]
books = ["pdf", "epub"]
"""


def test_rules_replace_the_built_in_categories(tmp_path):
    (tmp_path / ".organizer.toml").write_text(RULES, encoding="utf-8")
    for name in ("a.heic", "b.png", "c.epub", "d.pdf", "e.mp3"):
        make(tmp_path, name)
    assert main(["sort", str(tmp_path)]) == 0
    assert files(tmp_path) == ["books/c.epub", "books/d.pdf", "other/e.mp3", "photos/a.heic", "photos/b.png"]
    assert (tmp_path / ".organizer.toml").is_file(), "the rules file stays where it is"


def test_broken_rules_move_nothing(tmp_path, capsys):
    (tmp_path / ".organizer.toml").write_text("[categories\nphotos = [", encoding="utf-8")
    make(tmp_path, "a.png")
    assert main(["sort", str(tmp_path)]) == 1
    assert capsys.readouterr().err.startswith("error: bad rules: ")
    assert files(tmp_path) == ["a.png"]


def test_report_lists_categories_and_a_total(tmp_path, capsys):
    make(tmp_path, "documents/a.pdf", 1000)
    make(tmp_path, "documents/b.txt", 536)
    make(tmp_path, "images/c.png", 300)
    make(tmp_path, "loose.zip", 5000)
    assert main(["report", str(tmp_path)]) == 0
    assert capsys.readouterr().out.splitlines() == [
        "documents: 2 files, 1.5 KiB",
        "images: 1 file, 300 B",
        "total: 3 files, 1.8 KiB",
    ]
    assert (tmp_path / "loose.zip").is_file(), "report moves nothing"


def test_report_uses_bigger_units(tmp_path, capsys):
    make(tmp_path, "video/big.mp4", 3 * 1024 * 1024 + 512 * 1024)
    main(["report", str(tmp_path)])
    assert capsys.readouterr().out.splitlines() == ["video: 1 file, 3.5 MiB", "total: 1 file, 3.5 MiB"]


def test_an_empty_folder_reports_zero(tmp_path, capsys):
    assert main(["report", str(tmp_path)]) == 0
    assert capsys.readouterr().out.splitlines() == ["total: 0 files, 0 B"]


def test_report_of_a_missing_folder_is_an_error(tmp_path, capsys):
    assert main(["report", str(tmp_path / "nope")]) == 1
    assert capsys.readouterr().err.startswith("error: not a folder: ")
