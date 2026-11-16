---
from: python
status: draft
concept: "Capacity planning for a memory-bound service"
from_way: "Running a model with transformers in a script, one request at a time."
native_way: "Serving with an engine such as vLLM (python -m vllm.entrypoints.openai.api_server or vllm serve) that batches requests continuously and exposes an OpenAI-compatible API."
sensei_tip: "transformers' generate() is fine for experiments but serves one request at a time; vLLM on the same GPU can serve many times the throughput. Point the openai client's base_url at it and your code does not change."
---

| Python | Model serving |
| --- | --- |
| `pipeline("text-generation")` | experiments, one request at a time |
| `vllm serve <model>` | a production server |
| `openai.OpenAI(base_url=...)` | the same client for hosted and self-hosted |
| `bitsandbytes`, AWQ, GPTQ | quantized weights |
| `locust` | load testing |
