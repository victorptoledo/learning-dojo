# Sync a Feature Branch: Fetch, Rebase and Push Safely

Keeping a feature branch up to date is the most common thing people get wrong with remotes.
Here you automate it, and every refusal in the statement is a way real people lose work.
Press **Enter** to create the kata folder and open it.

The tests use a **bare repository** as the server (`git init --bare`): a repository without a
working tree, which is exactly what GitHub keeps on its side. Your script works in a clone
of it whose remote is called `origin`.
