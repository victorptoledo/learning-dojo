# Metaprompting and Prompt Optimisation

A **metaprompt** is a prompt whose output is another prompt. Models are good at writing
prompts: they know the conventions, they write long, structured instructions without getting
bored, and they can read a hundred failures faster than you. Used well, a metaprompt turns
prompt writing from guesswork into a loop you can measure. Used badly, it produces prompts that
look impressive, overfit the test set and break the code that fills them in.

## Where metaprompts help

- **First drafts.** Describe the task, the inputs and the output contract, and let a model draft
  the prompt (the consoles of the major providers include prompt generators built this way).
  You then edit and evaluate it like any other draft.
- **Rewrites.** Improve an existing prompt from a critique, port it to a new model family, or
  turn a long prompt into a shorter one with the same behaviour.
- **Briefs for other models.** An orchestrator that writes the brief for a sub-agent is running
  a metaprompt on every call (see the previous lesson).
- **Test data.** Generate varied inputs and edge cases for the evaluation set, then check them
  by hand: a model is a fine author of test cases and a poor judge of its own.

## The template contract

An application prompt is a **template**: code fills in `{{input}}`, `{{customer_name}}` or a
`<documents>` block. A rewritten template must keep that contract:

- every **placeholder** the code fills must still be there, spelled the same, and no new ones
  may appear that the code does not know;
- the **output format** the parser expects must not change (that would be a major version);
- the **variables** must stay where escaping and delimiting put them.

Check this **in code** before you run a single evaluation: extract the candidate from tags,
compare its placeholders with the current template's and reject it with a specific reason.
This is the same discipline as validating a tool call before running it.

## The improvement loop

The most useful pattern is an optimisation loop around an evaluation set:

1. score the current prompt on a **development** set and collect the failures;
2. give the optimiser model the current prompt and a **bounded sample** of failures (input,
   expected, got), escaped and delimited, and ask for an analysis followed by the complete new
   prompt in tags;
3. extract and check the candidate, score it on the development set, and keep it only if it is
   better by more than a margin;
4. repeat for a fixed number of rounds, or until nothing fails.

Two context-engineering choices make this work. Each round starts from a **fresh context** with
only the current best prompt and its current failures, not the growing history of every
attempt. And the failures are **capped** in number and length, because twenty long transcripts
bury the pattern you want the optimiser to see.

Research systems automate the same idea: APE generates and selects instructions, OPRO feeds
past prompts and their scores back to an optimiser model, and DSPy compiles prompts and
few-shot examples from a program and a metric.

## Overfitting and leakage

An optimiser optimises what you measure, nothing more:

- **Hold out data.** Split the labelled cases into a development set the optimiser sees and a
  **holdout** set it never sees. Accept the final prompt only if it does not do worse than the
  original on the holdout set. A prompt that improves on development but drops on holdout has
  memorised the development cases.
- **No leakage.** The expected answers in the failures are visible to the optimiser, so it may
  copy them into the prompt ("if the ticket mentions invoice 4411, answer billing"). Review the
  diff, and make sure holdout inputs never appear in any optimiser call.
- **Mind the grader.** If the grader is weak (a keyword match), the optimiser learns to please
  the grader, not the user. Use the graders from the evaluation lesson, and sample outputs by
  hand.
- **Mind the noise.** A gain of one case on a small set may be chance; require a margin and
  confirm with more data.

## Keep a human in the loop

Treat a generated prompt as a pull request from a fast, tireless colleague who has never met
your users. Read the diff, check it against the template contract, run the full evaluation,
record the change and its results in the changelog, and ship it like any other prompt version.

## Further reading

- Zhou et al., "Large Language Models Are Human-Level Prompt Engineers" (APE, 2022): <https://arxiv.org/abs/2211.01910>
- Yang et al., "Large Language Models as Optimizers" (OPRO, 2023): <https://arxiv.org/abs/2309.03409>
- Khattab et al., "DSPy: Compiling Declarative Language Model Calls into Self-Improving Pipelines" (2023): <https://arxiv.org/abs/2310.03714>
- Anthropic, "Use our prompt improver to optimize your prompts": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/prompt-improver>
- OpenAI, "Prompt generation" (meta-prompts): <https://platform.openai.com/docs/guides/prompt-generation>
