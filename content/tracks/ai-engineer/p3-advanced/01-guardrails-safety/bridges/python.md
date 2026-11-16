---
from: python
status: draft
concept: "Untrusted text that gives orders"
from_way: "Parameterised queries, escaping in templates (Jinja2 autoescape) and never calling eval on input."
native_way: "The same rules for model output, plus least-privilege tools, approvals for side effects and redaction of personal data before it reaches the prompt."
sensei_tip: "Microsoft Presidio (presidio-analyzer and presidio-anonymizer) detects and replaces many kinds of PII; still validate detections, like the Luhn check in the next kata, and test on your own data."
---

| Python | LLM guardrail |
| --- | --- |
| `cursor.execute(sql, params)` | model output as parameters, never as SQL |
| Jinja2 autoescape, `markupsafe` | escaping model output in HTML |
| `re` patterns plus validation | PII detection |
| decorators checking permissions | authorisation in tools |
| `logging.Filter` | masking data in logs |
