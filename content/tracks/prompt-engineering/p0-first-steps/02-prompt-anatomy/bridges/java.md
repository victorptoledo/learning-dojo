---
from: java
status: draft
concept: "A prompt template is a typed interface"
from_way: "String concatenation or String.format to build text, and a method signature that says exactly which arguments are needed."
native_way: "A prompt template with named, delimited sections; rendering checks that every variable is supplied, like a constructor that refuses missing arguments."
sensei_tip: "Write your template renderer like a strict formatter: a missing variable throws, an unknown one throws, and values are never parsed again. That is the difference between a template and string concatenation."
---

| Java | Prompt concept |
| --- | --- |
| `"Hello " + name` | ad-hoc prompt strings (avoid) |
| `String.format("%s", x)` / text blocks | a template with variables |
| method parameters | the template's named variables |
| `IllegalArgumentException` | fail on a missing variable |
| Javadoc on a method | the role, task and output-format sections |
