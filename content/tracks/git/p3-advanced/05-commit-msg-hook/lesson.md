# Commit Message Guard: a commit-msg Hook and Its Installer

Conventional Commits subjects (`fix(cart): round VAT per line`) feed changelogs and version
bumps, so teams guard them with a `commit-msg` hook. You write both halves: the hook, and the
installer every new clone runs. Press **Enter** to create the kata folder and open it.

The tests call your hook directly with message files, and also install it into real
repositories and run `git commit` to see Git refuse a bad message.
