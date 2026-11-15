# Team Workflows: Trunk-based, GitHub Flow and Pull Request Review

Git does not prescribe how a team uses branches. A **workflow** is that agreement: where work
starts, how it is reviewed, how it reaches production. The right one follows from **how you
release**.

## Trunk-based development

Everyone integrates into **one branch** (`main`, the trunk) at least daily, either directly
in small commits or through branches that live hours to a couple of days. Unfinished work is
merged behind **feature flags**, so `main` is always releasable. Continuous integration runs
on every change; releases are cut from `main` (or from short-lived release branches for
teams that must support versions).

It minimises merge pain (branches never diverge far) and is what high-performing delivery
teams in the DORA research report doing. It requires good tests and the discipline to keep
changes small.

## GitHub flow

A lightweight form of the same idea, built around pull requests:

1. Branch from `main` (`fix/vat-rounding`).
2. Commit and push early; open a **pull request** (draft if not ready).
3. Discuss, review, let CI run; push fixes to the same branch.
4. Merge into `main` when approved and green; deploy `main`; delete the branch.

## Gitflow and release branches

**Gitflow** uses long-lived `main` (released code) and `develop` (integration), plus
`feature/*`, `release/*` and `hotfix/*` branches with prescribed merges between them. It fits
products with **scheduled, versioned releases** and several supported versions (installed
software, mobile apps, libraries). For web services deployed continuously it adds ceremony
and long-lived branches that conflict; even its author later recommended simpler flows for
that case.

| | Trunk-based / GitHub flow | Gitflow |
| --- | --- | --- |
| Long-lived branches | `main` only | `main` and `develop` |
| Branch lifetime | hours to days | days to weeks |
| Unfinished work | feature flags | stays on feature branches |
| Fits | continuous delivery | scheduled releases, several versions |

## Protecting main

Hosts let you encode the workflow as rules (GitHub rulesets and branch protection, GitLab
protected branches):

- **Require a pull request** and N approving reviews; dismiss approvals when new commits
  arrive.
- **Require status checks** (build, tests, linters) to pass, on an up-to-date branch.
- **Block force-pushes and deletion**; optionally **require linear history** (only squash or
  rebase merges) and **signed commits**.
- **CODEOWNERS**: a file mapping paths to the people or teams who must review them
  (`/payments/ @acme/payments-team`).
- **Merge queue**: when many PRs land per hour, the queue tests each one on top of the
  others before merging, so `main` stays green without endless "update branch" rebases.

## Pull requests that get reviewed quickly

For the author:

- **Small**: one purpose, a few hundred lines at most. Split refactors from behaviour
  changes. Stack dependent PRs if needed.
- **Explained**: the description says *why*, what changed, how to test it, and links the
  issue. Screenshots for UI changes.
- **Self-reviewed**: read your own diff on the host before asking anyone. Clean history with
  fixups and autosquash when the team keeps commits.
- **Green**: CI passes before review is requested.

For the reviewer:

- Review **soon** (within hours): waiting PRs go stale and conflict.
- Focus on behaviour, design, tests, security and naming; leave formatting to tools.
- Ask questions instead of issuing orders, mark nits as nits, and approve when the remaining
  comments are optional.
- Check that the tests prove the change; pull the branch and run it when in doubt
  (`gh pr checkout 123`).

## Summary

- Choose the workflow from the release model: trunk-based or GitHub flow for continuous
  delivery, Gitflow-style release branches for versioned, scheduled releases.
- Feature flags let unfinished work merge safely, keeping branches short.
- Protect `main` with required reviews and checks, no force-push, CODEOWNERS and, at scale, a
  merge queue.
- Small, explained, self-reviewed pull requests and fast, respectful reviews keep the cycle
  short.

## Further reading

- GitHub Docs, "GitHub flow": <https://docs.github.com/en/get-started/using-github/github-flow>
- Pro Git, "Distributed Workflows": <https://git-scm.com/book/en/v2/Distributed-Git-Distributed-Workflows>
- Atlassian, "Comparing Git workflows": <https://www.atlassian.com/git/tutorials/comparing-workflows>
- Atlassian, "Trunk-based development": <https://www.atlassian.com/continuous-delivery/continuous-integration/trunk-based-development>
- trunkbaseddevelopment.com: <https://trunkbaseddevelopment.com/>
- GitHub Docs, "About code owners": <https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners>
- GitHub Docs, "About pull request reviews": <https://docs.github.com/en/pull-requests/reference/pull-request-reviews>
- roadmap.sh topic "Code Reviews": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/code-reviews@xLB2fhsOm0Vu3xg_PusJB.md>
