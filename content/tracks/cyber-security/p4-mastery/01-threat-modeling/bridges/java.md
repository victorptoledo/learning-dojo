---
from: java
status: draft
concept: "Ask what can go wrong before writing the code"
from_way: "Security reviewed at the end, by checking that Spring Security is configured."
native_way: "A short STRIDE session per risky feature at design time, with mitigations turned into tickets and tests (for example method-security tests for each role)."
sensei_tip: "Turn each elevation-of-privilege threat into a test: a `@WithMockUser(roles = \"VIEWER\")` MockMvc test that expects 403 on the edit endpoint keeps the mitigation from regressing."
---

| Java | Threat modeling habit |
| --- | --- |
| security review before release | threat model at design |
| architecture diagram without boundaries | a DFD with trust boundaries |
| "we use HTTPS" | threats per element and flow |
| findings in a document | tickets and tests |
| one model, never updated | revisited when the design changes |
