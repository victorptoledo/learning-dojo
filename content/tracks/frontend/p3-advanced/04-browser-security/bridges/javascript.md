---
from: javascript
status: draft
concept: "XSS outside the framework"
from_way: "React, Vue or Angular escape interpolations, so XSS feels solved."
native_way: "Escape hatches (dangerouslySetInnerHTML, v-html, bypassSecurityTrust*), href with user URLs, and third-party scripts still open holes."
sensei_tip: "Search your codebase for dangerouslySetInnerHTML, v-html and innerHTML, and for href={user...}: that is where XSS lives in framework apps."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `{value}` in JSX | `textContent`: escaped |
| `dangerouslySetInnerHTML` | `innerHTML`: needs a sanitizer |
| `href={user.website}` | Validate with `new URL()` first |
| helmet in Express | The same security headers |
