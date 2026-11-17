"""Tidies a downloads folder: one subfolder per kind of file."""

import sys
from dataclasses import dataclass
from pathlib import Path
import tomllib

DEFAULT_CATEGORIES = {
    "images": {"jpg", "jpeg", "png", "gif", "webp", "svg"},
    "documents": {"pdf", "docx", "txt", "md", "odt", "xlsx", "csv"},
    "archives": {"zip", "tar", "gz", "7z", "rar"},
    "audio": {"mp3", "wav", "flac", "ogg"},
    "video": {"mp4", "mkv", "mov", "webm"},
}
OTHER = "other"
USAGE = "usage: organizer (sort <folder> [--dry-run] | report <folder>)"
RULES_FILE = ".organizer.toml"

class OrganizerError(Exception):
    """A problem to report on stderr, with its exit code."""

    def __init__(self, message: str, code: int = 1) -> None:
        super().__init__(message)
        self.code = code

@dataclass(frozen=True)
class Move:
    source: Path
    category: str
    target: Path

def category_of(path: Path, categories: dict[str, set[str]]) -> str:
    extension = path.suffix.lower().lstrip(".")
    for name, extensions in categories.items():
        if extension and extension in extensions:
            return name
    return OTHER

def free_name(folder: Path, name: str) -> str:
    candidate = folder / name
    if not candidate.exists():
        return name
    stem, suffix = Path(name).stem, Path(name).suffix
    number = 1
    while (folder / f"{stem} ({number}){suffix}").exists():
        number += 1
    return f"{stem} ({number}){suffix}"

def plan(folder: Path, categories: dict[str, set[str]]) -> list[Move]:
    moves = []
    for path in sorted(folder.iterdir(), key=lambda p: p.name):
        if not path.is_file() or path.name.startswith("."):
            continue
        category = category_of(path, categories)
        target = folder / category / free_name(folder / category, path.name)
        moves.append(Move(path, category, target))
    return moves

def load_categories(folder: Path) -> dict[str, set[str]]:
    rules = folder / RULES_FILE
    if not rules.is_file():
        return DEFAULT_CATEGORIES
    try:
        data = tomllib.loads(rules.read_text(encoding="utf-8"))
        table = data["categories"]
        return {name: {str(ext).lower().lstrip(".") for ext in exts} for name, exts in table.items()}
    except (tomllib.TOMLDecodeError, KeyError, TypeError, AttributeError) as error:
        raise OrganizerError(f"error: bad rules: {error}") from error

def human_size(size: int) -> str:
    if size < 1024:
        return f"{size} B"
    value = float(size)
    for unit in ("KiB", "MiB", "GiB"):
        value /= 1024
        if value < 1024 or unit == "GiB":
            return f"{value:.1f} {unit}"
    raise AssertionError("unreachable")

def files_word(count: int) -> str:
    return "1 file" if count == 1 else f"{count} files"

def report(folder: Path) -> None:
    total_files = total_size = 0
    for category in sorted(p for p in folder.iterdir() if p.is_dir() and not p.name.startswith(".")):
        sizes = [p.stat().st_size for p in category.iterdir() if p.is_file()]
        if not sizes:
            continue
        print(f"{category.name}: {files_word(len(sizes))}, {human_size(sum(sizes))}")
        total_files += len(sizes)
        total_size += sum(sizes)
    print(f"total: {files_word(total_files)}, {human_size(total_size)}")

def sort(folder: Path, dry_run: bool) -> None:
    categories = load_categories(folder)
    moves = plan(folder, categories)
    if not moves:
        print("nothing to sort")
        return
    for move in moves:
        shown = f"{move.category}/" if move.target.name == move.source.name else f"{move.category}/{move.target.name}"
        if dry_run:
            print(f"would move {move.source.name} -> {shown}")
            continue
        move.target.parent.mkdir(exist_ok=True)
        move.source.rename(move.target)
        print(f"moved {move.source.name} -> {shown}")

def main(argv: list[str]) -> int:
    try:
        if len(argv) < 2 or argv[0] not in ("sort", "report"):
            raise OrganizerError(USAGE, 2)
        command, folder, options = argv[0], Path(argv[1]), argv[2:]
        if options not in ([], ["--dry-run"]) or (command == "report" and options):
            raise OrganizerError(USAGE, 2)
        if not folder.is_dir():
            raise OrganizerError(f"error: not a folder: {folder}")
        if command == "report":
            report(folder)
            return 0
        sort(folder, dry_run=options == ["--dry-run"])
        return 0
    except OrganizerError as error:
        print(error, file=sys.stderr)
        return error.code

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
