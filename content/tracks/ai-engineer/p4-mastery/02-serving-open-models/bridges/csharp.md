---
from: csharp
status: draft
concept: "Capacity planning for a memory-bound service"
from_way: "Hosting ASP.NET Core services and sizing App Service plans or pods."
native_way: "Running open models with an inference server (or ONNX Runtime GenAI locally) and calling them through IChatClient implementations for OpenAI-compatible endpoints or Ollama."
sensei_tip: "Because IChatClient abstracts the provider, you can develop against Ollama on your laptop and deploy against a vLLM cluster or a hosted API with a configuration change."
---

| .NET | Model serving |
| --- | --- |
| App Service plan sizing | GPU sizing |
| ONNX Runtime GenAI | local inference |
| an OpenAI-compatible `IChatClient` | calling a self-hosted model |
| Azure Load Testing | measuring TTFT and throughput |
| deployment slots | switching model versions |
