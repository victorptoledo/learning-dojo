# Evaluating and Iterating on Prompts

"It looks better" is not evidence. A prompt change that fixes the three inputs you tried can
break thirty you did not. **Evaluation** turns prompt work into engineering: a fixed set of
inputs, a way to grade outputs, numbers you can compare, and a rule for shipping.

## From criteria to a test set

Start from the success criteria you wrote in Fundamentals. Then collect inputs to test them:

| Kind of case | Why | Where from |
| --- | --- | --- |
| **typical** | the bulk of real traffic | sampled from logs (anonymised) |
| **edge** | empty, very long, other languages, odd formats | logs, and invented on purpose |
| **adversarial** | injection attempts, attempts to break the rules | your red-team list, OWASP examples |
| **regression** | every bug you fixed | each incident adds a case |

Tag cases with **slices** (language, length, customer type, kind of edge case) so that you can
see results per slice. An overall pass rate of 92% can hide a slice at 40%.

Size: dozens of cases catch big problems; a few hundred let you see differences of a few
percent. Keep the evaluation cases **separate from the few-shot examples**, and never tune the
prompt on the exact cases you report.

## Graders

Grade with the cheapest method that is reliable for the criterion:

| Grader | Good for | Limits |
| --- | --- | --- |
| **exact match** | labels, numbers, ids | brittle for free text |
| **contains / pattern** | a required phrase, a forbidden word, a format | shallow |
| **structure** | valid JSON, required keys, schema, length | says nothing about meaning |
| **model-graded** (LLM-as-judge) | faithfulness, tone, helpfulness with a rubric | cost, bias, needs validation |
| **human review** | the ground truth, ambiguous cases | slow and expensive |

**Model graders** need care:

- give them a **rubric** with concrete criteria and examples of each score, and ask for
  reasoning before the verdict;
- prefer **binary or small scales** ("faithful: yes/no") over 1-to-10 scores;
- known biases: **position** (in pairwise comparisons, preferring the first or second answer;
  swap and grade twice), **verbosity** (preferring longer answers), **self-preference**
  (preferring outputs of the same model family);
- **validate** the grader against human labels on a sample before you trust it, and again when
  you change it.

## Variance

Model outputs vary, so one run per case gives a noisy number. Run each case several times (or
at least the cases near the boundary), and treat small differences with suspicion. A change
from 81% to 83% on 50 cases is well within noise; on 1,000 cases with several runs it may be
real.

## Error analysis

The most valuable part of evaluation is **reading the failures**:

1. take the failed cases (and a sample of passes);
2. write down what went wrong in a few words for each;
3. group them into categories ("ignored the length limit", "invented a date", "answered in the
   wrong language");
4. fix the largest category first, then re-run **everything**.

Often the fix is not in the wording: a missing fact, a missing example, a step that should be
split, a grader that is wrong.

## The iteration loop

```text
criteria → test set → baseline run → error analysis → ONE change → full re-run → compare
     ↑                                                                             │
     └──────────────────────────── keep or revert ◀────────────────────────────────┘
```

Change one thing at a time so you know what helped. Compare the candidate to the baseline
**case by case**: the cases it fixed, and, more important, the cases it **broke** (regressions).
Decide in advance what may ship: for example "no regressions on critical cases, no slice drops
by more than 2 points, and the overall pass rate does not go down". The kata in Mastery builds
exactly this comparison.

## Offline and online

Everything above is **offline** evaluation, before shipping. After shipping, keep measuring
**online**: user feedback, escalation rates, sampled human review, and the same graders run on a
sample of live traffic. New failure modes from production become new test cases.

## Further reading

- Anthropic, "Define your success criteria": <https://docs.anthropic.com/en/docs/test-and-evaluate/define-success>
- Anthropic, "Create strong empirical evaluations": <https://docs.anthropic.com/en/docs/test-and-evaluate/develop-tests>
- OpenAI, "Evaluation best practices": <https://platform.openai.com/docs/guides/evaluation-best-practices>
- Zheng et al., "Judging LLM-as-a-Judge with MT-Bench and Chatbot Arena" (2023), on grader biases: <https://arxiv.org/abs/2306.05685>
- Hamel Husain, "Your AI Product Needs Evals": <https://hamel.dev/blog/posts/evals/>
- Shankar et al., "Who Validates the Validators? Aligning LLM-Assisted Evaluation of LLM Outputs with Human Preferences" (2024): <https://arxiv.org/abs/2404.12272>
