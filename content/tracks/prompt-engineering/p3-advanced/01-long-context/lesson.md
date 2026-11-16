# Long Context

Context windows have grown from a few thousand tokens to hundreds of thousands and more. That
makes new things possible (ask questions about a whole contract, a code base, a year of support
tickets) and creates new ways to fail. A long prompt is not just a short prompt with more text:
layout, grounding and budget all start to matter.

## Layout: documents first, question last

Put the long material at the **top**, and the instructions and the question at the **end**:

```text
<documents>
<document index="1" id="pol-12" title="Travel policy" updated="2026-03-01">
...
</document>
<document index="2" id="pol-07" title="Expenses policy" updated="2025-11-20">
...
</document>
</documents>

Answer the question using only the documents above. Quote the passages you
rely on first.

<question>
Can I expense a taxi from the airport after 22:00?
</question>
```

Provider guides report measurably better answers with this order on long inputs, and it is
also the order that caches well (the documents change less often than the question).

**Label every document.** Give each one tags and metadata: an index, an id, a title, a date,
a source. The model can then cite them ("pol-12"), tell versions apart ("the 2026 policy
replaces the 2025 one") and your code can check the citations. Escape the document text so
that a document cannot close its own tag.

## Ground the answer: quote first

On long inputs, ask the model to **extract the relevant quotes first**, then answer from them:

```text
First, find the passages that answer the question and copy them word for word
into <quotes> tags, each with the id of its document. Then answer in <answer>
tags, using only those quotes. If no passage answers the question, say so.
```

Quoting focuses the model on the relevant parts of a large input, and it makes the answer
**checkable**: your code can verify that every quote really occurs in the documents. A quote
that does not exist is a strong signal of a hallucinated answer.

## Lost in the middle

Research found that models use information at the **beginning and the end** of a long context
better than information in the **middle**. Newer models are much better at long inputs, but the
effect has not gone away, and "needle in a haystack" tests (find one sentence) are easier than
real tasks that combine several facts spread through a long text. Practical consequences:

- put the **most relevant** documents first, not in arbitrary order;
- do not pad the context with material that is probably irrelevant: it dilutes attention and
  costs money;
- for tasks that need **every** item (summarise all 200 tickets), split the work: process
  chunks, then combine (a map-reduce chain).

## Budgets and truncation

The context window holds input **and** output, and every token costs. Decide the budget, then
fit the material into it on purpose:

1. reserve the output tokens and the fixed parts (instructions, question);
2. order documents by relevance;
3. include whole documents while they fit;
4. truncate the next one at a word or paragraph boundary and **mark** the cut (`[truncated]`)
   so the model knows the text is incomplete;
5. leave the rest out, and record what was left out (it matters when you debug a wrong answer).

The kata after this lesson implements exactly this policy.

## Prompt caching

Providers can **cache** a prompt prefix that repeats across calls: the documents and
instructions of a long prompt are processed once and reused, which cuts the cost of the cached
part and the time to the first token. Caching works on **identical prefixes**, so:

- stable content first (system prompt, tools, documents), varying content last (the question);
- do not put timestamps, request ids or per-user data near the start;
- keep document order deterministic (sort by a stable key when scores tie).

## Stuffing or retrieving

A long window does not mean you should always fill it. Compare:

| Put everything in the prompt | Retrieve relevant passages (RAG) |
| --- | --- |
| simple; nothing can be missed by retrieval | cheaper and faster per call |
| fine for one long document or a small corpus | needed for large or changing corpora |
| cost and latency grow with every document | retrieval can miss the right passage |

Building retrieval systems is part of the AI Engineer track. The prompt side is the same
either way: labelled documents first, quotes, and the question last.

## Further reading

- Anthropic, "Long context prompting tips": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/long-context-tips>
- Liu et al., "Lost in the Middle: How Language Models Use Long Contexts" (2023): <https://arxiv.org/abs/2307.03172>
- Anthropic, "Prompt caching": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-caching>
- OpenAI, "Prompt caching": <https://platform.openai.com/docs/guides/prompt-caching>
- Google, "Long context" for Gemini: <https://ai.google.dev/gemini-api/docs/long-context>
- Hsieh et al., "RULER: What's the Real Context Size of Your Long-Context Language Models?" (2024): <https://arxiv.org/abs/2404.06654>
