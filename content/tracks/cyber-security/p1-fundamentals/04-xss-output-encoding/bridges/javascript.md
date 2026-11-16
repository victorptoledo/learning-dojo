---
from: javascript
status: draft
concept: "Encode for the context the data lands in"
from_way: "element.innerHTML = userText, and dangerouslySetInnerHTML for rich text."
native_way: "textContent and JSX for text, DOMPurify for HTML, URL parsing for links, and Trusted Types to lock down DOM sinks."
sensei_tip: "React escapes text in JSX, but not URLs: `<a href={userUrl}>` with `javascript:` still runs. Parse with `new URL()` and allow only http and https."
---

| JavaScript / TypeScript | XSS-safe way |
| --- | --- |
| el.innerHTML = text | el.textContent = text |
| dangerouslySetInnerHTML={{__html: bio}} | DOMPurify.sanitize(bio) first |
| <a href={userUrl}> | new URL(userUrl).protocol check |
| eval / new Function on data | JSON.parse |
| no CSP | CSP with nonces and Trusted Types |
