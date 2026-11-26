# Context Engineering for Agents

A single prompt is written once. An **agent** works in a loop: it calls the model, runs the tools
the model asked for, adds the results to the conversation and calls the model again, often
dozens or hundreds of times. On every call the whole context is sent again. **Context
engineering** is the work of deciding what is in that context on each turn: which instructions,
which tools, which memories, which part of the history and which tool results.

Prompt engineering is still part of it: a context engineer also writes the system prompt and
the tool descriptions. The new part is that the context **changes and grows** while the agent
works, so you need rules for what goes in, what stays and what leaves.

## Context is a budget with diminishing returns

The context window is a hard limit, but quality drops long before it is reached. Studies of long
inputs show that models find and use information less reliably as the context grows, and that
distracting, similar-looking text makes it worse (sometimes called **context rot**). Every token
also costs money and latency, on every turn.

On a long task, the biggest consumer is rarely your prompt. It is the **tool results**: file
contents, search results, logs, test output. After an hour, a coding agent may carry thirty test
runs, of which only the last one matters. Keep the **smallest set of high-signal tokens** that
lets the model do the next step well.

## The parts of an agent's context

| Part | Changes | Advice |
| --- | --- | --- |
| system prompt | rarely | the role, goals, constraints and how to work, at the right altitude |
| tool definitions | rarely | few tools, clear names, no overlapping purposes |
| memory and notes | sometimes | durable facts the agent wrote down, read back when needed |
| history | every turn | recent turns verbatim, older turns summarised |
| tool results | every turn | the newest ones in full, older ones cleared or shortened |

**The right altitude.** A system prompt that hard-codes long if-then rules for every case is
brittle; one that says "be a helpful engineer" gives no guidance. Aim between them: say what
matters, the constraints and their reasons, and the heuristics to use, organised in clear
sections (for example `<background>`, `<rules>`, `<how_to_work>`), and let the model handle the
cases you did not foresee.

## Keeping a long conversation in budget

Three techniques, cheapest first:

1. **Clear old tool results.** Replace the content of tool results older than the last few with
   a short placeholder such as `[result cleared: 1840 tokens]`. Keep the tool call and its
   result **paired**: provider APIs reject a tool result without its call, and the model loses
   track of what it did. The model can run the tool again if it needs the data.
2. **Compact the history.** When the context nears a threshold, replace the older turns with a
   summary and keep the recent turns verbatim. A good summary keeps decisions and their reasons,
   the user's constraints, open problems, and identifiers (file paths, ids, URLs) the agent will
   need; it drops raw output and dead ends. **Pin** what must never be summarised away, such as
   the user's original task. Never cut between a tool call and its result.
3. **Take notes outside the window.** Let the agent write durable facts to a notes file or a
   memory tool (a to-do list, findings, decisions) and read them back later, even in a new
   session. The notes survive compaction because they are not in the history at all.

Compaction loses information by definition, so test it: put long tasks in your evaluation set
and check that constraints given early are still followed late.

## Just-in-time context

Instead of loading every document up front, give the agent **lightweight references** (file
paths, record ids, URLs, a directory listing) and tools to open them when needed. People work
the same way: you do not read the whole repository before fixing a bug; you search, open a few
files and keep notes. Up-front loading still wins for small, stable material that is needed on
almost every turn, and a mix of both is common.

## Sub-agents and briefs

A **sub-agent** is a model call (or a whole agent loop) with its **own, fresh context**. The main
agent hands it a focused task, the sub-agent may read hundreds of files, and only a condensed
result comes back. This keeps the main context clean and lets independent work run in parallel.

The sub-agent knows only what its **brief** says, so write it like a task for a new colleague:

- the **goal** and why it matters;
- the **context** it needs (paths, ids, what has been tried), not the whole history;
- the **constraints** (read-only, do not change the public API, time or token limits);
- the **output format**: what to return, how long, with which evidence.

Vague briefs are a common failure of multi-agent systems: sub-agents duplicate work, miss
constraints, or return pages of raw output that refill the main context.

## Caching and stability

Prompt caching makes a stable **prefix** cheap. Put what changes least first (system prompt,
tools, pinned material) and what changes most last. Compaction and clearing change the prefix
and **invalidate the cache** from that point on, so do them rarely and in larger steps, not a
little on every turn.

## Further reading

- Anthropic, "Effective context engineering for AI agents" (2025): <https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents>
- Anthropic, "How we built our multi-agent research system" (2025): <https://www.anthropic.com/engineering/multi-agent-research-system>
- Chroma Research, "Context Rot: How Increasing Input Tokens Impacts LLM Performance" (2025): <https://research.trychroma.com/context-rot>
- Liu et al., "Lost in the Middle: How Language Models Use Long Contexts" (2023): <https://arxiv.org/abs/2307.03172>
