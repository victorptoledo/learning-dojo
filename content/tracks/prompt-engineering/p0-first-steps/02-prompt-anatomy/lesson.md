# The Anatomy of a Prompt

Most weak prompts fail for the same reason: the model has to guess. It guesses who the answer
is for, what counts as good, where the instructions end and the data begins, and what shape
the answer should have. A good prompt removes the guessing by making each of those things
explicit. This lesson takes a prompt apart into its pieces.

## The parts

| Part | What it says | Example |
| --- | --- | --- |
| **Role** | who the model is acting as, for whom | "You review pull requests for a payments team." |
| **Task** | the one thing to do, stated as an action | "Summarise the customer email below." |
| **Context** | background the model cannot know | "Our refund window is 30 days." |
| **Rules** | constraints and priorities | "Never promise a refund. Under 80 words." |
| **Examples** | demonstrations of input and ideal output | two sample emails with their summaries |
| **Input** | the data to work on, clearly delimited | the email itself |
| **Output format** | the exact shape of the answer | "Reply with JSON: {\"summary\": ..., \"urgent\": true or false}" |

Not every prompt needs every part. A quick question needs a task and maybe a format. A prompt
that runs ten thousand times a day inside an application usually needs all of them.

## A weak prompt and a strong one

```text
summarize this and tell me if its urgent
Hi, my card was charged twice for order 1182 and I need the money back before Friday...
```

The model has to guess the length, the audience, what "urgent" means and the shape of the
answer, and nothing marks where the instruction ends and the email begins.

```text
You help the support team of an online bookshop triage customer emails.

<context>
An email is urgent when the customer reports money taken wrongly, a legal threat,
or a deadline within 3 days.
</context>

<email>
Hi, my card was charged twice for order 1182 and I need the money back before Friday...
</email>

Summarise the email for a support agent in at most two sentences, then decide
whether it is urgent by the definition in <context>.

Reply in exactly this format:
summary: <one or two sentences>
urgent: yes | no
```

## Delimiters keep data apart from instructions

When instructions and data share one text, the model has to work out which is which. Make it
obvious with delimiters:

- **XML-style tags** such as `<email>...</email>` or `<document>...</document>`. Models from
  several providers are trained to respect them, they nest, and you can refer to them by name
  ("by the definition in `<context>`"). This track uses them throughout.
- **Markdown headings** (`## Rules`) or fenced blocks for longer, readable prompts.
- **Triple quotes** or other fences for a single block of text.

There is nothing magic about the tag names: pick descriptive ones and use them consistently.
Delimiting has one more benefit: it is the first line of defence against **prompt injection**,
which you will study in Advanced. If an email contains "ignore your instructions", a clearly
fenced `<email>` block makes it more likely that the model treats it as data. (Delimiters
alone are not a defence, and data must be escaped so it cannot close the tag; more on that
later.)

## Order matters

The model reads the prompt top to bottom and every token conditions the next, so placement is
a design decision:

- Put **long material** (documents, data) **before** the question and the final instructions.
  Provider guides report better answers when the query comes at the end of a long prompt.
- Put the **task and the output format close to the end**, where they are fresh when the
  model starts writing.
- Keep the **stable parts first** (role, rules, examples) and the parts that change per call
  last. Besides reading well, this lets providers **cache** the shared prefix, which makes
  repeated calls cheaper and faster.

## From prompt to template

A prompt used by code is a **template**: fixed text with named variables filled in per call.

```python
TEMPLATE = """You help the support team of {{shop}} triage customer emails.

<email>
{{email}}
</email>

Summarise the email for a support agent in at most {{max_sentences}} sentences."""
```

Treat templates like code:

- every variable has a name, and rendering **fails loudly** when one is missing (a prompt
  with a literal `{{email}}` in it is a silent bug);
- values are inserted **literally**: an email that contains `{{shop}}` must not be expanded;
- the template lives in version control with a name and a version, not pasted in strings
  across the code base (the versioning lesson in Mastery covers this).

## Say what to do, not only what not to do

"Do not write long answers" leaves the model to guess what you want instead. "Answer in at most
three sentences" tells it. Positive, specific instructions work better than prohibitions, and
when a rule matters, give the reason: "Write plain text, because the answer is read aloud by a
speech engine" lets the model generalise the rule to cases you did not list.

## Checklist

Before you ship a prompt, check that it answers these questions:

1. Who is the answer for, and what will they do with it?
2. What exactly is the task, in one sentence?
3. What does the model need to know that it cannot know?
4. Where does the data start and end?
5. What shape must the answer have, and how will code read it?
6. What does a good answer look like? (If you cannot say, you cannot test it.)

## Further reading

- Anthropic, "Use XML tags to structure your prompts": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/use-xml-tags>
- Anthropic, "Be clear, direct, and detailed": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/be-clear-and-direct>
- OpenAI, "Prompt engineering": message formatting with Markdown and XML: <https://platform.openai.com/docs/guides/prompt-engineering>
- Google, "Prompt design strategies": <https://ai.google.dev/gemini-api/docs/prompting-strategies>
- Prompt Engineering Guide, "Elements of a Prompt": <https://www.promptingguide.ai/introduction/elements>
