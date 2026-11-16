# Guardrail Filter: PII, Injections, Tool Policy and Output Checks

No single check stops prompt injection or data leaks, so production systems stack several cheap,
deterministic layers around the model. You build four of them: reversible PII redaction, injection
detection and safe wrapping of untrusted text, a least-privilege tool policy, and an output check
that catches exfiltration links. Press **Enter** to create the kata folder and open it.
