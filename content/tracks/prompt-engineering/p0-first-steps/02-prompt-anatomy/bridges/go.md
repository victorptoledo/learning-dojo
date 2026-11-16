---
from: go
status: draft
concept: "text/template for prompts"
from_way: "fmt.Sprintf with positional verbs, or text/template, which prints <no value> for a missing key unless you set missingkey=error."
native_way: "A prompt template with named, delimited parts that fails on a missing variable and inserts values literally."
sensei_tip: "If you build prompts with text/template, always call Option(\"missingkey=error\"); the default silently writes <no value> into the prompt."
---

| Go | Prompt concept |
| --- | --- |
| `fmt.Sprintf("Summarise %s", email)` | an inline prompt |
| `template.New("p").Option("missingkey=error")` | a template that fails on a missing variable |
| a struct passed to `Execute` | the template's typed variables |
| `embed` a `.tmpl` file | a template kept in a versioned file |
| `html/template` escaping | escaping data before it enters the prompt |
