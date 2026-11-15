---
from: javascript
status: draft
concept: "Dependency injection"
from_way: "Modules importing singletons (`import db from './db'`) and tests replacing modules with jest.mock."
native_way: "Factory functions that take their dependencies (`createTransferService({ accounts, rates })`) wired in one entry file; NestJS or InversifyJS when a container is wanted."
sensei_tip: "NestJS's module and provider system is close to Spring's: constructor injection by type, scopes, and providers registered per module."
---

| JavaScript | DI idea |
| --- | --- |
| `createService({ db, clock })` | constructor (factory) injection |
| `index.js` / `main.ts` wiring | composition root |
| NestJS `@Injectable()` | container |
| `jest.mock('./db')` | sign of hidden dependencies |
| passing `Date.now` as a parameter | time as a dependency |
