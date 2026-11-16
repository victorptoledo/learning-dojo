---
from: go
status: draft
concept: "Capacity planning for a memory-bound service"
from_way: "Small, efficient services where CPU and memory per instance are cheap."
native_way: "Expensive GPU servers whose capacity is limited by memory for weights and KV cache, fronted by lightweight Go gateways that route, queue and rate-limit."
sensei_tip: "Ollama is written in Go and wraps llama.cpp; read its API to see what a local inference server exposes. In production, a Go gateway in front of vLLM replicas handles auth, quotas and routing."
---

| Go | Model serving |
| --- | --- |
| a reverse proxy (`httputil.ReverseProxy`) | a gateway in front of inference servers |
| `GOMAXPROCS` and pools | concurrent sequences per GPU |
| Ollama | local inference |
| vegeta or k6 | load tests |
| a base URL in config | switching between hosted and self-hosted |
