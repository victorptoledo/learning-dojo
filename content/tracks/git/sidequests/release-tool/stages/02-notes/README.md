`release.sh notes REPO` prints the notes of the next release in Markdown: what users of the
project need to know, grouped by kind of change, from the same commits `next` reads.

```text
$ bash release.sh notes ~/src/shop
## v2.0.0 (2024-03-09)

### ⚠ BREAKING CHANGES

- **api:** the /vouchers endpoint is gone (3f2a9c1)

### Features

- **cart:** add coupon codes (1b7e44d)
- search by SKU (9d0c2aa)

### Bug Fixes

- round VAT per line (5e81f07)
```

- The title is `## VERSION (DATE)`: the version `next` prints and the date of the commit at
  `HEAD` (its committer date, `YYYY-MM-DD`).
- Then, in this order, the sections that have entries: `### ⚠ BREAKING CHANGES`,
  `### Features` (`feat`), `### Bug Fixes` (`fix`) and `### Performance` (`perf`). Other types
  (`docs`, `chore`, `refactor`...) are not in the notes. A section with no entries is left out.
- Each entry is `- DESCRIPTION (HASH)`, or `- **SCOPE:** DESCRIPTION (HASH)` when the commit
  has a scope, where `HASH` is the first 7 characters of the commit's hash. Entries keep the
  order of history, oldest first.
- A breaking change is listed under `⚠ BREAKING CHANGES` with the text of its
  `BREAKING CHANGE:` footer (or its description when it only has a `!`), **and** under its own
  type like any other commit.
- One empty line after the title and after each section heading, and one between sections;
  no empty line at the end.
- The first release (`v0.1.0`) may have no `feat`, `fix` or `perf` at all: then the title is
  followed by an empty line and `No user-facing changes.`

Errors are the ones of `next`: nothing to release or no commits exits `1` with the same
message, and nothing on stdout.
