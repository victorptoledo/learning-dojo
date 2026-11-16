---
from: javascript
status: draft
concept: "Capacity planning for a memory-bound service"
from_way: "Deploying Node services that scale horizontally on cheap CPU instances."
native_way: "GPU-backed inference servers that scale in expensive steps, called through an OpenAI-compatible endpoint, plus small models running locally or even in the browser."
sensei_tip: "Small quantized models can run in the browser with WebGPU (for example with transformers.js or WebLLM); useful for privacy-sensitive, offline or very low-latency features, at modest quality."
---

| JavaScript | Model serving |
| --- | --- |
| horizontal scaling of Node pods | scaling GPU replicas |
| `openai` client with `baseURL` | calling a self-hosted model |
| transformers.js, WebLLM | models in the browser |
| k6 load tests | measuring TTFT and throughput |
| CDN edge functions | edge or local inference |
