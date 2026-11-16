---
from: javascript
status: draft
concept: "Template literals versus prompt templates"
from_way: "Template literals (`Hello ${name}`) that quietly print undefined when a value is missing."
native_way: "A prompt template with named, delimited parts whose renderer rejects missing variables and inserts values literally."
sensei_tip: "A template literal with an undefined value gives you the text \"undefined\" in the prompt, and the model will try to make sense of it. Validate the variables before you render."
---

| JavaScript | Prompt concept |
| --- | --- |
| `` `Summarise ${email}` `` | an inline prompt |
| `${x}` with `x === undefined` | a silent missing variable |
| a tagged template function | a renderer that can validate and escape |
| JSX keeping markup and data apart | delimiters keeping instructions and data apart |
| `const PROMPT = ...` in one module | a template kept in one place |
