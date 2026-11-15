# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a `land.sh` that does what a "rebase and
   merge" button does, from fixups to cleanup, and refuses every state that could lose work.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Rebase vs Merge | What happens to commit hashes in a rebase? Which branches must you never rebase? How does the reflog undo a rebase? |
| Remotes | What moves `origin/main`? Why must a push be a fast-forward? What does `--force-with-lease` compare? |
| Conflicts | What do the three sections of a zdiff3 conflict show? Which side is "ours" during a rebase? |
| Rewriting History | What do `fixup` and `squash` do? How does `--autosquash` know where a fixup goes? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
