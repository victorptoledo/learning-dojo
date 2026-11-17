"""Stage 1 contract: `sort` moves top-level files into one folder per category."""

from pathlib import Path

from organizer import main


def make(folder: Path, *names: str) -> None:
    for name in names:
        path = folder / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f"content of {name}", encoding="utf-8")


def files(folder: Path) -> list[str]:
    return sorted(p.relative_to(folder).as_posix() for p in folder.rglob("*") if p.is_file())


def test_files_go_to_their_category_folder(tmp_path):
    make(tmp_path, "cat.png", "report.pdf", "song.mp3", "movie.mkv", "backup.zip")
    assert main(["sort", str(tmp_path)]) == 0
    assert files(tmp_path) == [
        "archives/backup.zip",
        "audio/song.mp3",
        "documents/report.pdf",
        "images/cat.png",
        "video/movie.mkv",
    ]


def test_content_is_kept(tmp_path):
    make(tmp_path, "notes.txt")
    main(["sort", str(tmp_path)])
    assert (tmp_path / "documents" / "notes.txt").read_text(encoding="utf-8") == "content of notes.txt"


def test_unknown_and_missing_extensions_go_to_other(tmp_path):
    make(tmp_path, "setup.exe", "README")
    main(["sort", str(tmp_path)])
    assert files(tmp_path) == ["other/README", "other/setup.exe"]


def test_extensions_ignore_case_and_only_the_last_one_counts(tmp_path):
    make(tmp_path, "PHOTO.JPG", "backup.tar.gz", "data.Csv")
    main(["sort", str(tmp_path)])
    assert files(tmp_path) == ["archives/backup.tar.gz", "documents/data.Csv", "images/PHOTO.JPG"]


def test_folders_and_hidden_files_are_left_alone(tmp_path):
    make(tmp_path, "projects/plan.pdf", ".hidden.txt", "photo.webp")
    main(["sort", str(tmp_path)])
    assert files(tmp_path) == [".hidden.txt", "images/photo.webp", "projects/plan.pdf"]


def test_each_move_is_printed_in_name_order(tmp_path, capsys):
    make(tmp_path, "zebra.png", "alpha.pdf", "Mid.zip")
    main(["sort", str(tmp_path)])
    assert capsys.readouterr().out.splitlines() == [
        "moved Mid.zip -> archives/",
        "moved alpha.pdf -> documents/",
        "moved zebra.png -> images/",
    ]


def test_an_existing_category_folder_is_reused(tmp_path):
    make(tmp_path, "images/old.png", "new.png")
    main(["sort", str(tmp_path)])
    assert files(tmp_path) == ["images/new.png", "images/old.png"]


def test_nothing_to_sort_says_so(tmp_path, capsys):
    make(tmp_path, "images/old.png")
    assert main(["sort", str(tmp_path)]) == 0
    assert capsys.readouterr().out.strip() == "nothing to sort"
