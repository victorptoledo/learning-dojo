---
from: python
status: draft
concept: "A model call is a remote call with a richer result"
from_way: "requests.post(...).json() and raise_for_status(), maybe with tenacity for retries."
native_way: "client.messages.create(...) or client.chat.completions.create(...) from the official SDKs, with typed responses, built-in retries and typed exceptions."
sensei_tip: "Use the official SDKs (anthropic, openai, google-genai): they handle retries, streaming helpers and types. Wrap them in a small function of your own that returns your neutral Response, as the katas do."
---

| Python habit | Model API |
| --- | --- |
| `resp.json()["text"]` | read `content` only after checking `stop_reason` |
| `raise_for_status()` | typed errors: `RateLimitError`, `APIStatusError`, `APIConnectionError` |
| `for line in resp.iter_lines()` | SDK streaming: `with client.messages.stream(...) as s` |
| `os.environ["KEY"]` | the SDK reads `ANTHROPIC_API_KEY` / `OPENAI_API_KEY` by default |
| `unittest.mock` | a fake client returning scripted responses |
