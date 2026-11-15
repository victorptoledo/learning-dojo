# Version Control, Installing and Configuring Git

Every team that writes software needs answers to the same questions: what changed, who changed
it, why, and how do we get back to the version that worked yesterday? A **version control
system** (VCS) records the history of a set of files so those questions have answers.

## From copies to distributed history

| Generation | Example | How it works | Weakness |
| --- | --- | --- | --- |
| Copies | `report-final-v2-REAL.docx` | duplicate the folder | no history, no merging |
| Local VCS | RCS | one database of versions per machine | no collaboration |
| Centralised | Subversion, Perforce | one server holds the history, clients check out files | the server is a single point of failure; most commands need the network |
| Distributed | **Git**, Mercurial | every clone holds the **whole history** | large binary files need extra tools |

Git was written by Linus Torvalds in 2005 for the Linux kernel. Because every clone is a full
repository, almost every command (`log`, `diff`, `commit`, `branch`) runs **offline and
fast**, and every clone is a backup. Sharing happens by exchanging commits between
repositories (`fetch`, `push`), usually through a hosting service such as **GitHub**,
GitLab or Bitbucket. Git is the tool; GitHub is a service that hosts Git repositories and
adds pull requests, issues and CI.

## Installing

| System | Command |
| --- | --- |
| Windows | `winget install Git.Git` (Git for Windows, with Git Bash) |
| Debian / Ubuntu | `sudo apt install git` |
| Fedora | `sudo dnf install git` |
| macOS | `xcode-select --install` or `brew install git` |

Check it with `git --version`. This track uses Git 2.47 or newer, the version in Debian 13.

## Configuration in three levels

Git reads its settings from three files. The **most specific one wins**:

| Level | Flag | File | Applies to |
| --- | --- | --- | --- |
| system | `--system` | `/etc/gitconfig` | every user of the machine |
| global | `--global` | `~/.gitconfig` (or `~/.config/git/config`) | you, in every repository |
| local | `--local` (the default) | `.git/config` | this repository only |

The first-day setup:

```bash
git config --global user.name "Ada Lovelace"
git config --global user.email "ada@example.com"   # the address your host knows
git config --global init.defaultBranch main
git config --global core.editor "code --wait"      # or nano, vim, notepad
git config --global pull.rebase false              # say what pull does (Phase 2)
```

`user.name` and `user.email` are written into **every commit** you make; they are not a
login. A work repository can override the email locally:
`git config user.email ada@company.com`.

To see what is set and **where it comes from**:

```bash
git config --list --show-origin     # every value with the file it came from
git config --show-scope user.email  # system, global or local
git config --get core.editor
```

## Line endings

Windows editors write `CRLF` line ends, Linux and macOS `LF`. If each machine commits its own,
every line looks changed. Two ways to stay sane:

- `git config --global core.autocrlf true` on Windows: convert to LF on commit and back to
  CRLF on checkout (`input` on Linux and macOS: convert on commit only).
- Better, a `.gitattributes` file in the repository, so the rule travels with the code:

```text
* text=auto
*.sh text eol=lf
*.cmd text eol=crlf
*.png binary
```

## A first repository

```bash
mkdir notes && cd notes
git init                 # creates .git/, the repository itself
echo "# Notes" > README.md
git status               # README.md is untracked
git add README.md        # stage it
git commit -m "Add README"
git log --oneline        # 1a2b3c4 Add README
```

Everything Git knows lives in the `.git` directory. Delete it and the folder is just files
again; copy it and you have copied the whole history.

## How the dojo checks git katas

Git katas are **bash scripts** that drive `git`. The checker runs them in the `dojo-linux`
container (Debian 13 with bash, Git, bats and ShellCheck). Each test builds a small
repository fixture in a temporary folder, runs your script against it and then inspects
the result: the log, the branches, the tags and the files. The tests set a throwaway
identity (`Dojo Tester`), so your scripts must never call `git config --global`.

## Summary

- A VCS answers what changed, who, why and how to go back. Git is distributed: every clone
  holds the whole history, so most commands run offline.
- Git is the tool; GitHub and friends host repositories and add collaboration features.
- Configuration has system, global and local levels; the most specific wins, and
  `--show-origin` tells you which file set a value.
- Set `user.name`, `user.email`, `init.defaultBranch` and a line-ending policy on day one.

## Further reading

- Pro Git, "About Version Control": <https://git-scm.com/book/en/v2/Getting-Started-About-Version-Control>
- Pro Git, "First-Time Git Setup": <https://git-scm.com/book/en/v2/Getting-Started-First-Time-Git-Setup>
- git-config reference: <https://git-scm.com/docs/git-config>
- gitattributes reference: <https://git-scm.com/docs/gitattributes>
- roadmap.sh, the Git and GitHub roadmap: <https://roadmap.sh/git-github>
- roadmap.sh topic "Local vs Global Config": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/local-vs-global-config@tVvxC5JQgUb_B8kOqYpD8.md>
