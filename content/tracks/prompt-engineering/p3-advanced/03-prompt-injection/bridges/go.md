---
from: go
status: draft
concept: "Untrusted input everywhere"
from_way: "database/sql placeholders, html/template's automatic escaping, and least-privilege service accounts."
native_way: "Escape and delimit untrusted text before it enters the prompt, flag suspicious content, keep tools minimal, and check the output before it is rendered or acted on."
sensei_tip: "url.Parse(u).Hostname() plus a suffix check against an allow-list is all an output link check needs; compare with a leading dot so evilexample.com does not pass for example.com."
---

| Go | Prompt-injection concept |
| --- | --- |
| `db.Query(sql, args...)` | no exact equivalent: escape and delimit instead |
| `html.EscapeString` | escaping untrusted text |
| `unicode.IsControl` | finding hidden characters |
| `url.Parse(u).Hostname()` | checking output links |
| a service account with minimal rights | least privilege for tools |
