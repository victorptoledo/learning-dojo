---
from: javascript
status: draft
concept: "Bundling for browsers"
from_way: "Node.js runs modules from node_modules directly; bundling is optional on the server."
native_way: "Browsers need resolved URLs, and users pay for every byte, so frontend builds bundle, split and minify."
sensei_tip: "The Node habit of importing a whole utility library is expensive in a page; import only what you use and check the analyzer."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `node app.js` | `vite` dev server |
| `node --watch` | Hot module replacement |
| Big dependencies are cheap on a server | Every KB costs download and parse time |
| `process.env.SECRET` | No secrets in frontend code |
