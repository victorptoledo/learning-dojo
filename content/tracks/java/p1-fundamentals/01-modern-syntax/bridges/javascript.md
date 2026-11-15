---
from: javascript
status: draft
concept: "Everyday syntax: inference, strings and switch"
from_way: "let and const, template literals, switch with break, and typeof checks."
native_way: "var for local inference, text blocks, switch expressions without fall-through, and type patterns."
sensei_tip: "Java's arrow switch never falls through, unlike JavaScript's switch, and as an expression it must cover every case, like a TypeScript exhaustive check with never."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| let x = 10 | var x = 10; |
| `multi
line` | a text block |
| `Hi ${name}` | "Hi %s".formatted(name) |
| switch with break | switch with -> |
| typeof o === 'string' | o instanceof String s |
