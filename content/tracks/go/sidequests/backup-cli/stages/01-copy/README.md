`backup run <src> <dst>` copies every file under `<src>` (in every subfolder) to the same
place under `<dst>`, creating `<dst>` and the folders it needs, and exits with 0.

It prints one line per file copied, in path order, then a summary:

```text
copied notes.txt
copied photos/cat.jpg
summary: 2 copied, 0 unchanged
```

The copies have the same bytes as the originals. Every run copies everything in this stage.
