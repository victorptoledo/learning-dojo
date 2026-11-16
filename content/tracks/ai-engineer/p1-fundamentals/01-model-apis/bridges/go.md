---
from: go
status: draft
concept: "A model call is a remote call with a richer result"
from_way: "http.NewRequestWithContext, json.NewDecoder(resp.Body).Decode, checking resp.StatusCode."
native_way: "The official Go SDKs (anthropic-sdk-go, openai-go) with typed params, streaming iterators and errors you inspect with errors.As."
sensei_tip: "Pass a context.Context with a deadline to every call and inspect *anthropic.Error (or the OpenAI equivalent) with errors.As to decide on retries by status code."
---

| Go | Model API |
| --- | --- |
| `resp.StatusCode` | status plus the stop reason in the body |
| `errors.As(err, &apiErr)` | classify retryable versus fatal |
| `bufio.Scanner` over the body | SSE events, or the SDK's stream iterator |
| `os.Getenv("KEY")` | the key from the environment |
| an interface plus a fake struct | a fake model client for tests |
