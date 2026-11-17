"""Tidies a downloads folder: one subfolder per kind of file."""

import sys
from dataclasses import dataclass
from pathlib import Path

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

def plan(folder: Path, categories: dict[str, set[str]]) -> list[Move]:
    moves = []
    for path in sorted(folder.iterdir(), key=lambda p: p.name):
        if not path.is_file() or path.name.startswith("."):
            continue
        category = category_of(path, categories)
        target = folder / category / path.name
        moves.append(Move(path, category, target))
    return moves

def sort(folder: Path, dry_run: bool) -> None:
    categories = DEFAULT_CATEGORIES
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
        folder, options = Path(argv[1]), argv[2:]
        sort(folder, dry_run=options == ["--dry-run"])
        return 0
    except OrganizerError as error:
        print(error, file=sys.stderr)
        return error.code

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
