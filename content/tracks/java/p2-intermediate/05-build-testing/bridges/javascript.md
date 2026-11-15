---
from: javascript
status: draft
concept: "Builds and unit tests"
from_way: "jest or vitest with describe/it/expect, and npm scripts."
native_way: "JUnit 6 test classes, assertions, and Maven's test phase."
sensei_tip: "describe becomes a test class (or @Nested), it becomes @Test with a @DisplayName, and expect(...).toThrow becomes assertThrows."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| npm test | mvn test |
| describe('...') | class ...Test or @Nested |
| it('...') | @Test void ...() |
| expect(a).toBe(b) | assertEquals(b, a) |
| it.each | @ParameterizedTest |
