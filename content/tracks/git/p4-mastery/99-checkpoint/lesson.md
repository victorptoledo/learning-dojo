# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards large
   repositories, signing and secrets, team workflows and recovery. You need 7 right.
2. **The integration kata** (press **Enter**): a `pre-receive` hook for a team's server. It
   keeps `main` fast-forward-only and linear, keeps release tags annotated and immutable, and
   stops oversized files and leaked keys anywhere in the pushed history, rejecting the whole
   push with every problem listed.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Large Repositories | Why do big files stay in every clone forever once committed? What does a partial clone skip, and what does Git LFS put in history instead? |
| Signing and Secrets | What does a signature prove, and what does it not? Why is the first step after a leak rotating the secret? Why is server-side protection more reliable than a pre-commit scanner? |
| Team Workflows | What do "require linear history" and "block force pushes" protect? Why are a merge queue and short-lived branches the answer to slow, conflicting pull requests? |
| Recovering from Mistakes | Where is a commit after a bad reset, rebase or branch deletion? Why can a force-push to `main` lose work that no reflog on your machine has? |

The kata also leans on earlier phases: the object model of Phase 1 (`rev-list --objects` and
`cat-file` read commits, trees and blobs directly), the fast-forward of Phase 2, and the tags
and hooks of Phase 3.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
