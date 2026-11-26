# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a `hotfix.sh` that a release manager runs when
   a release is broken. It bisects the range between two release tags, reverts the culprit on
   a hotfix branch, checks the fix and tags the next patch version, all in a worktree of its
   own, and leaves nothing behind when any step fails.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Finding Regressions with git bisect | Why test both ends before searching? What do exit codes 0, 125, 1 and 128 mean to `bisect run`? Where is the answer kept before `bisect reset`? |
| Tags, Versions and Releases | How do you tell an annotated tag from a lightweight one? Which part of the version does a fix bump? Why is a published tag never moved? |
| Git Hooks | Where does Git look for hooks? Which commands commit **without** running `pre-commit` and `commit-msg`? |
| Worktrees, Submodules and Subtrees | What does a worktree share with the main checkout (objects, branches, hooks) and what is its own (HEAD, index, bisect)? |

The kata puts the four lessons in one script, so plan the order before you write it: every
step that creates something (a worktree, a branch, a bisect, a half-done revert) needs an
undo for the case where a later step fails. A `trap ... EXIT` that runs one `cleanup`
function is the usual shape.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
