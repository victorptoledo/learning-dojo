---
from: java
status: draft
concept: "Markup as a typed model"
from_way: "Domain classes with meaningful types instead of Map<String, Object> everywhere."
native_way: "Semantic elements are the types of a document: <nav>, <article> and <time> tell every consumer what the content is, while <div> is an untyped Object."
sensei_tip: "A div soup is like a codebase of Object and casts: it runs, but no tool can help you. Choose the most specific element, as you would choose the most specific type."
---

| Java | In the browser |
| --- | --- |
| `record Product(...)` instead of a Map | `<article class="product">` instead of a div |
| `LocalDate` | `<time datetime>` |
| `BigDecimal` price | `<data value="49.90">` |
| Javadoc on a type | `alt`, `<caption>`, `<legend>` |
