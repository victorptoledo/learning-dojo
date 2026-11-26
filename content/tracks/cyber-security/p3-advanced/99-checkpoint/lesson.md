# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a release gate that reviews a cloud IAM
   policy and a CI workflow, raises detections from authentication events and writes log
   lines that cannot be forged or leak secrets.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Security logging and detection | What must never be logged? How do you stop a username from forging a log line? Why count distinct accounts per IP? |
| Brute-Force Detector (kata) | What is inside the window and what is not? What happens to the counter after a success? |
| Hardening servers and services | What is a baseline? Why does the effective configuration matter more than the file? |
| sshd Audit (kata) | Which settings fail open when they are missing? |
| Web hardening | Which headers and cookie attributes matter, and what does a strict CSP look like? |
| Security Headers Audit (kata) | Why parse case-insensitively and take only the first value? |
| Cloud security and the supply chain | Why OIDC instead of access keys? What does pinning by SHA or digest protect against? What can a fork's pull request reach? |

> [!TIP]
> The kata's Part 2 (the CI workflow) goes one step past the lesson with two classic GitHub
> Actions mistakes: script injection through `${{ github.event... }}` and
> `pull_request_target` checking out the fork's code. The README explains both.
