# Decomposition and Prompt Chaining

A model given one prompt with five jobs usually does all five a little worse, and when the
result is wrong you cannot tell which job failed. The fix is the oldest idea in engineering:
**split the problem**. Each prompt gets one job, each output is checked, and the next prompt
starts from a known-good input.

## When to split

Split when:

- the task has **distinct stages** (extract, then analyse, then write);
- different stages need **different instructions or formats** (JSON for one, prose for another);
- you need to **inspect or validate** an intermediate result;
- one part is easy and cheap, another hard (use a small model for the first);
- the single prompt keeps **dropping one of the instructions**.

Keep one prompt when the task is genuinely one step, when latency matters more than
quality, or when the steps need each other's full context so much that splitting loses it.

## Patterns

**Sequential chain.** The output of step 1 is the input of step 2:

```text
document ──▶ [extract quotes] ──▶ <quotes> ──▶ [answer using quotes] ──▶ answer
```

**Gate between steps.** After each step, code checks the output before going on: the JSON
validates, the list is not empty, every quote really appears in the document. A failed gate
retries the step or stops the chain. Gates are where chains beat single prompts: errors are
caught where they happen.

**Routing.** A first, cheap call classifies the input, and code sends it to a specialised
prompt: refunds to the refund prompt, bug reports to the bug prompt. Each specialised prompt
stays short and focused.

**Parallel sections.** Independent parts run at the same time: review a pull request once for
security and once for performance, then merge the findings.

**Voting.** The same prompt runs several times (or with variations) and the answers are
combined by majority or by a judge. This is self-consistency, from the reasoning lesson.

**Generate, critique, refine.** One call drafts, a second critiques against the criteria, a
third rewrites. Useful when you have clear criteria; wasteful when you do not.

When the steps are not known in advance and a model decides them at run time (calling tools,
spawning sub-tasks), you have an **agent**. Building agents is the subject of the AI Engineer
track; the prompts inside an agent use everything in this one.

## Hand-offs between steps

The output of one step is the input of another prompt, so it needs the same care as any data:

- give each step an output format the next one can parse (tags or JSON);
- pass only what the next step needs, not the whole history;
- **delimit and escape** the previous output when you put it into the next prompt: it is model
  output, and if step 1 read an untrusted document, its output can carry injected instructions;
- keep a **trace**: for each step, the prompt, the raw answer, the parsed output, the number of
  attempts and the tokens. Debugging a chain without traces is guesswork.

## Costs

Chaining is not free:

| Cost | Why |
| --- | --- |
| latency | sequential steps add up; parallelise what you can |
| tokens | every call pays for its own context; shared material is sent more than once |
| code | each step needs a template, a parser, a gate and tests |
| error compounding | a 95%-reliable step five times in a row is about 77% reliable end to end |

The last row is why gates matter: they turn silent compounding errors into retries or visible
failures. It is also why you budget: a chain should have a token budget and stop cleanly when
it is exhausted, instead of retrying forever.

## Least-to-most

A related prompting technique is to ask the model itself to decompose: first "list the
sub-questions you need to answer", then answer them one by one, each with the earlier answers
in context. Research calls this **least-to-most prompting**. It helps on problems harder than
the examples you can show; in an application, the same idea is usually more reliable as an
explicit chain you control.

## Further reading

- Anthropic, "Chain complex prompts for stronger performance": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/chain-prompts>
- Anthropic, "Building effective agents" (workflows: chaining, routing, parallelisation): <https://www.anthropic.com/engineering/building-effective-agents>
- OpenAI, "Prompt engineering", split complex tasks: <https://platform.openai.com/docs/guides/prompt-engineering>
- Zhou et al., "Least-to-Most Prompting Enables Complex Reasoning in Large Language Models" (2022): <https://arxiv.org/abs/2205.10625>
- Madaan et al., "Self-Refine: Iterative Refinement with Self-Feedback" (2023): <https://arxiv.org/abs/2303.17651>
