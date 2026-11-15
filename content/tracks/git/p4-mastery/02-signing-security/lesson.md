# Signing Commits and Keeping Secrets out of History

Two security questions every team meets: **who really made this commit?** and **what did we
accidentally publish?**

## Author fields prove nothing

`user.name` and `user.email` are plain text you choose. Anybody can commit as anybody:

```bash
git -c user.name="Grace Hopper" -c user.email="grace@example.com" commit -m "Trust me"
```

The host shows that commit with Grace's avatar if the email matches her account. Pushing is
authenticated (your SSH key or token), but the commit itself is not.

## Signing commits and tags

A **signature** is computed over the commit object with a private key; anyone with the
public key can verify it. Since Git 2.34 you can sign with the **SSH key** you already use,
which is the simplest setup:

```bash
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true      # sign every commit
git config --global tag.gpgsign true         # and every annotated tag

git commit -S -m "Sign this one explicitly"
git tag -s v2.0.0 -m "Release 2.0.0"
```

To verify locally, Git needs to know which keys belong to whom (an "allowed signers" file):

```bash
echo "ada@example.com $(cat ~/.ssh/id_ed25519.pub)" >> ~/.config/git/allowed_signers
git config --global gpg.ssh.allowedSignersFile ~/.config/git/allowed_signers
git log --show-signature -1
git verify-commit HEAD
git verify-tag v2.0.0
```

GPG (`gpg.format openpgp`, the default) and X.509 (`gpg.format x509`, with S/MIME
certificates) work the same way. On GitHub and GitLab you upload the public key as a
**signing key**; commits then show **Verified**, and a ruleset can **require signed
commits** on protected branches. Vigilant mode marks your unsigned commits as Unverified, so
forgeries stand out.

Signatures say **who** made a commit. They do not say the code is safe, reviewed or free of
secrets.

## Secrets: prevention

A secret that reaches a public repository is found by scanners within minutes. Prevention
has layers:

- Keep secrets out of the working tree: environment variables, a secret manager,
  `.env` files in `.gitignore` with an `.env.example` committed instead.
- A secret scanner in a `pre-commit` hook (gitleaks, detect-secrets, trufflehog) catches
  most mistakes on the laptop.
- **Server-side push protection** (GitHub secret scanning, GitLab secret push protection)
  rejects pushes containing known secret formats, and cannot be skipped with
  `--no-verify`.

## Secrets: response

If a secret was pushed, **assume it is compromised**. The order matters:

1. **Rotate or revoke it now.** Rewriting history takes time and does not undo the leak:
   clones, forks, caches and scanners already have it.
2. **Remove it from history** if the repository will stay, so it is not found again later:

   ```bash
   # git filter-repo is a separate tool, recommended over git filter-branch.
   git filter-repo --invert-paths --path config/prod.env          # drop a file everywhere
   git filter-repo --replace-text expressions.txt                 # or replace the secret text
   git push --force --all && git push --force --tags
   ```

3. **Coordinate**: every hash after the bad commit changed. Everyone re-clones (or resets
   their branches onto the new history); an old clone merged back reintroduces the secret.
   Ask the host's support to purge cached views and pull request refs if needed.
4. **Prevent** the next one: the layers above.

A secret only in **unpushed** local commits is simpler: amend or interactive-rebase it away
before pushing, and still rotate it if there is any doubt.

## Other hardening worth knowing

- Protect `main`: no force-pushes, no deletion, required reviews and checks (next lesson).
- `transfer.fsckObjects true` makes Git verify every object it receives.
- Review new dependencies and submodule URLs like code: they are code.

## Summary

- Author name and email are unauthenticated text; signatures with SSH, GPG or X.509 keys
  prove who made a commit or tag.
- Configure `gpg.format ssh`, `user.signingkey` and `commit.gpgsign`; verify with
  `git log --show-signature` and an allowed-signers file; hosts show Verified and can require
  signatures.
- Keep secrets out with ignore files, pre-commit scanners and server-side push protection.
- A pushed secret is leaked: rotate first, then rewrite with `git filter-repo`, force-push and
  have everyone re-clone.

## Further reading

- Pro Git, "Signing Your Work": <https://git-scm.com/book/en/v2/Git-Tools-Signing-Your-Work>
- git-config reference, gpg.format and gpg.ssh.allowedSignersFile: <https://git-scm.com/docs/git-config>
- GitHub Docs, "About commit signature verification": <https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification>
- GitHub Docs, "About secret scanning": <https://docs.github.com/en/code-security/concepts/secret-security/secret-scanning>
- GitHub Docs, "Removing sensitive data from a repository": <https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository>
- git filter-repo: <https://github.com/newren/git-filter-repo>
