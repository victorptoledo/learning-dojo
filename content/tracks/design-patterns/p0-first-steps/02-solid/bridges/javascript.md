---
from: javascript
status: draft
concept: "SOLID principles"
from_way: "One `utils.js` with everything, modules importing the database client at the top, `Date.now()` inline."
native_way: "Modules split by reason to change, dependencies passed as function or constructor arguments, and small object shapes (or TypeScript interfaces) for what a caller needs."
sensei_tip: "Module-level imports of I/O clients are the JavaScript version of `new` inside a service: pass the client in, and the test needs no module mocking."
---

| JavaScript | SOLID idea |
| --- | --- |
| one ES module per concern | single responsibility |
| a registry object `{ pdf: renderPdf, csv: renderCsv }` | open/closed |
| a subclass whose method throws | a Liskov warning sign |
| a function that takes `{ read }` | interface segregation |
| `createService({ db, clock })` | dependency inversion |
