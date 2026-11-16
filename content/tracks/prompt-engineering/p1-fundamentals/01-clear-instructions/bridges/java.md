---
from: java
status: draft
concept: "A prompt as a specification"
from_way: "A Javadoc comment and a method contract with preconditions, postconditions and documented exceptions."
native_way: "A prompt states the task, the context, the constraints with their reasons, the output contract and what to return when the input does not fit."
sensei_tip: "Write the success criteria like the assertions of a unit test before you write the prompt; if you cannot assert it, you have not specified it."
---

| Java | Prompt concept |
| --- | --- |
| a method contract in Javadoc | the task and output format |
| preconditions | what input the prompt expects |
| `@throws IllegalArgumentException` | the explicit exit (`NOT_AN_ORDER`) |
| assertions in a JUnit test | success criteria |
| a code comment explaining why | the reason behind a rule |
