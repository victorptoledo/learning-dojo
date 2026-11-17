`main(["sort", folder])` moves every file at the top of `folder` into a subfolder named after
its category, creating it when needed, and returns 0:

| Category | Extensions |
|---|---|
| `images` | `jpg` `jpeg` `png` `gif` `webp` `svg` |
| `documents` | `pdf` `docx` `txt` `md` `odt` `xlsx` `csv` |
| `archives` | `zip` `tar` `gz` `7z` `rar` |
| `audio` | `mp3` `wav` `flac` `ogg` |
| `video` | `mp4` `mkv` `mov` `webm` |
| `other` | anything else, and files without an extension |

- Extensions compare ignoring case: `PHOTO.JPG` is an image. Only the last extension counts
  (`backup.tar.gz` is `gz`, an archive).
- Folders already inside `folder` are left alone, and so are hidden files (names starting
  with `.`).
- For each file moved, in name order, print `moved <name> -> <category>/`.
- A folder with nothing to move prints `nothing to sort` and returns 0.
