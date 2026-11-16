# Clear Instructions and Context

A useful test for any prompt: **would a smart colleague who has never seen your project do the
task well with only this text?** A model is that colleague. It is capable and widely read, but
it knows nothing about your users, your product, your style guide or why the task exists. Every
gap you leave, it fills with the most generic plausible choice.

## Start from success criteria

Before you write the prompt, write what a good output is, in terms someone could check:

| Vague goal | Success criteria |
| --- | --- |
| "Good summaries" | at most 3 sentences; mentions the customer's request and any deadline; no advice |
| "Classify tickets well" | one of four labels; 95% agreement with the support lead on 200 labelled tickets |
| "Friendly replies" | greets by name; under 120 words; never promises refunds; reading age about 12 |

Criteria do two jobs. They tell you what to put in the prompt, and they become the tests you run
later (the evaluation lesson in Advanced builds on exactly this list). If you cannot write the
criteria, the prompt is not the problem yet: the task is not defined.

## Be specific

Replace adjectives with facts:

- "short" → "at most 50 words";
- "not too technical" → "for customers who do not program: no code, no internal service names";
- "the important parts" → "the price, the delivery date and anything the customer must do";
- "a list" → "a Markdown list, one item per change, most important first".

Use **numbered steps** when order matters, and say what to do at each step. Use one prompt for
one job: a prompt that asks for a summary, a translation and a sentiment score at once tends to
do all three worse (chaining, in Intermediate, splits such jobs).

## Positive instructions, with reasons

Tell the model what to do rather than only what to avoid. "Do not use Markdown" leaves the
format open; "Write plain paragraphs of text" closes it. When a rule exists for a reason, say
the reason:

```text
Write numbers as digits ("3 items", not "three items"), because the text is
parsed by a script that extracts quantities.
```

A model that knows *why* applies the rule to cases you did not list (it will also write "12
days", not "twelve days"). Without the reason, a rule is applied narrowly or inconsistently.
Also avoid SHOUTING and long chains of "NEVER" and "ALWAYS": current models follow instructions
closely, and over-emphasis makes them apply a rule too broadly, at the expense of others.

## Give the context it cannot know

Context is the information that makes a generic answer specific:

- **audience and purpose**: who reads the output and what they do next;
- **domain facts**: your refund policy, your product names, today's date;
- **where the output goes**: a chat bubble, a JSON parser, an email, a speech engine;
- **what was tried**: "previous summaries were too long and repeated the subject line".

Put facts in the prompt instead of hoping the model knows them. "Our premium plan costs 12
euros a month" in the context beats any amount of "do not make up prices".

## Give it a way out

If every input must produce an answer in your format, the model will produce one, even when
the input is empty, in another language, off-topic or ambiguous. Give an explicit exit:

```text
If the email is not about an order (for example spam or a job application),
reply with exactly: NOT_AN_ORDER
If a field cannot be found in the email, write "unknown" instead of guessing.
```

An exit turns a silent wrong answer into a visible, countable signal your code can handle.

## Length, style and tone

Models mirror the prompt. A prompt written in clipped bullet points tends to get clipped
answers; a prompt full of Markdown gets Markdown back. If you want flowing prose, write the
instructions in prose. Ask for the length you want directly, and if the output still drifts,
show an example of the right length (the few-shot lesson).

## Iterate like an engineer

Write the prompt, run it on a handful of real inputs (including awkward ones), read every
output, and compare them with the criteria. When something is wrong, ask what the model was
missing: a fact, a definition, an exit, an example? Change **one thing** at a time so you know
what helped. Keep the inputs that broke it: they are the start of your test set.

## Further reading

- Anthropic, "Be clear, direct, and detailed": <https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/be-clear-and-direct>
- Anthropic, "Define your success criteria": <https://docs.anthropic.com/en/docs/test-and-evaluate/define-success>
- OpenAI, "Prompt engineering", writing clear instructions: <https://platform.openai.com/docs/guides/prompt-engineering>
- Google, "Prompt design strategies": give clear and specific instructions: <https://ai.google.dev/gemini-api/docs/prompting-strategies>
- Prompt Engineering Guide, "General Tips for Designing Prompts": <https://www.promptingguide.ai/introduction/tips>
