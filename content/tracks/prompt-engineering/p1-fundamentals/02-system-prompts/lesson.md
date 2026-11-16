# Roles, Messages and System Prompts

Chat APIs take a list of messages, and each message has a **role**. The roles are not
decoration: models are trained to give them different weight. Using them well is the first
step from a prompt that works in a playground to a prompt that works in an application.

## The roles

| Role | Written by | Holds |
| --- | --- | --- |
| `system` (some APIs call the top level `developer`) | you, the operator | the standing instructions: role, scope, rules, format, tools |
| `user` | the end user, or your code on their behalf | the request of this turn, plus data for it |
| `assistant` | the model (or you, to show an example) | the model's earlier answers |
| `tool` / tool results | your code | the output of a tool the model asked to call (Intermediate) |

The APIs differ in shape (Anthropic takes `system` as a separate parameter; OpenAI's newer
APIs use `developer` and `instructions`), but the idea is the same everywhere.

## The instruction hierarchy

Models are trained to follow an **instruction hierarchy**: the provider's own policies first,
then the operator's system prompt, then the user. When the user says "ignore your rules", a
well-trained model keeps following the system prompt. This makes the system prompt the right
place for anything the user should not be able to override: the scope, the safety rules, the
output contract.

The hierarchy is a strong tendency, not a security boundary. Determined users and text injected
through documents can still get around it (the prompt-injection lesson). So:

- never put **secrets** (API keys, internal URLs, other customers' data) in a system prompt;
  assume a user can make the model reveal it;
- enforce **permissions in code**, not only in words: if the assistant must not refund over
  50 euros, the refund tool must refuse it.

## What goes in the system prompt

Good system prompts contain what is **stable across calls**:

- the **role** and the **audience** ("You are the in-app assistant of Acme Bank. You talk to
  retail customers on a phone screen.");
- the **scope**: what to help with, and what to do with everything else;
- **rules** with reasons, and the **output format**;
- **tools** and when to use them;
- long-lived **reference material** (a policy, a product catalogue), which caches well.

Keep **per-call data** in the user turn: the user's message, the record you fetched for this
request, today's search results. That data changes every call (so it would break prompt
caching), it is often untrusted, and putting it next to the operator's rules blurs the
hierarchy.

## Role prompting

"You are an experienced tax accountant" is **role prompting**. A role helps when it brings a
set of expectations: vocabulary, level of detail, what to check, what to be careful about. It
helps most when it is specific and paired with an audience: "You are a senior site-reliability
engineer explaining an outage to non-technical executives" carries much more than "You are a
helpful assistant".

Roles do not add knowledge. "You are a world-class lawyer" does not make the legal facts more
accurate; context and sources do. Use the role for tone, focus and judgement, and supply facts
separately.

## Multi-turn conversations

The model sees the whole message list on every call. Consequences:

- **earlier turns are instructions too**: if the assistant answered in French three turns ago,
  it will tend to continue in French;
- **history costs tokens** on every call; long chats need trimming or a summary of old turns;
- you can **write assistant turns yourself** to show the model an example exchange (few-shot
  as conversation), as long as you label them honestly in your own code.

Messages must usually alternate `user` and `assistant`; check your API's rules for
consecutive messages of the same role.

## Prefilling the answer

Some APIs let you end the message list with the start of the assistant's answer, for example
`{` to force JSON or `<summary>` to skip a preamble. The model continues from there. Prefill is
powerful but not universal: several providers and newer models do not support it, and
structured-output features (next lessons) are usually the better way to force a format today.

## A system prompt skeleton

```text
You are the in-app assistant of Acme Bank. You help retail customers on a phone
screen with questions about cards, transfers and the app itself.

<scope>
Help with: card status, blocking a lost card, transfer limits, app settings.
For anything else (investments, loans, complaints), say you cannot help with
that here and offer to connect the customer to a human agent.
</scope>

<style>
Answer in at most three short paragraphs of plain text: the screen is small,
and tables and headings do not render.
</style>

<rules>
- Use only the account facts given in the <account> block of the user turn.
- If an answer depends on facts you do not have, say so and offer a human agent.
</rules>
```

## Further reading

- Anthropic, "Giving Claude a role with a system prompt": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/system-prompts>
- OpenAI, "Text generation": message roles and instructions: <https://platform.openai.com/docs/guides/text>
- OpenAI, Model Spec (the chain of command between platform, developer and user): <https://model-spec.openai.com/>
- Wallace et al., "The Instruction Hierarchy" (2024): <https://arxiv.org/abs/2404.13208>
- Anthropic, "Prefill Claude's response": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/prefill-claudes-response>
