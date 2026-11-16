# Serving Open-Weight Models: Inference Engines, Quantization and GPUs

Hosted APIs are the default, and for good reason: someone else runs the GPUs. But many teams run
**open-weight models** (models whose weights are published, such as the Llama, Mistral, Qwen, Gemma
and DeepSeek families) themselves. This lesson gives you the vocabulary and the arithmetic to
decide, and to talk to whoever runs the hardware.

## Why self-host, and why not

| Reasons to self-host | Reasons not to |
| --- | --- |
| Data must not leave your infrastructure (regulation, contracts) | The best models are often hosted only |
| Very high, steady volume where GPUs are cheaper than per-token prices | GPUs are expensive when idle; traffic is bursty |
| Control: pinned versions, custom fine-tunes, no deprecations | You operate everything: capacity, upgrades, security, on-call |
| Latency at the edge or offline use | Quality and features (tool use, long context) may lag behind |

A middle path: open-weight models on managed inference platforms or your cloud provider's model
service, with data processed in your chosen region.

## What limits inference

**Memory for the weights.** Roughly `parameters × bytes per parameter`:

| Precision | Bytes per parameter | 8B model | 70B model |
| --- | --- | --- | --- |
| 16-bit (FP16 / BF16) | 2 | about 16 GB | about 140 GB |
| 8-bit | 1 | about 8 GB | about 70 GB |
| 4-bit | 0.5 | about 4 GB | about 35 GB |

**Memory for the KV cache.** While generating, the model keeps keys and values for every token of
every active sequence. It grows with **context length × concurrent requests**, and for long contexts
it can exceed the weights. It is what limits how many users one GPU can serve.

**Memory bandwidth.** Decoding one token reads all the weights from GPU memory, so single-stream
speed is bounded by bandwidth, not compute. Serving many requests **together** reuses each read for
the whole batch, which is why throughput rises with batching while per-request speed falls a little.

## Quantization

Storing weights in fewer bits (8-bit, 4-bit; formats such as GPTQ, AWQ, GGUF, FP8) cuts memory and
often speeds up decoding, at some loss of quality, small at 8-bit, more noticeable at 4-bit on hard
tasks and small models. Always **evaluate the quantized model** on your golden set; do not assume.

## Inference engines

- **vLLM**, **SGLang**, **TensorRT-LLM**, **Hugging Face TGI**: production servers with
  **continuous batching** (new requests join the running batch at every step instead of waiting for
  a batch to finish), **paged attention** (the KV cache is stored in fixed-size blocks like virtual
  memory pages, so little memory is wasted), prefix caching, quantization support and tensor
  parallelism across GPUs.
- **llama.cpp** and **Ollama**: run quantized models on laptops, CPUs and small GPUs; great for
  development, offline tools and edge cases.
- Most expose an **OpenAI-compatible HTTP API**, so your thin client (Fundamentals) can switch between
  a hosted model and your own server by changing a base URL and a model name.

Further speed-ups: **speculative decoding** (a small draft model proposes several tokens that the big
model verifies in one pass), and splitting prefill and decode onto different machines at large scale.

## Sizing and comparing

1. Measure your workload: requests per second at peak, prompt and output lengths, latency targets.
2. Pick candidate models and precisions; check quality on the golden set first.
3. Load-test the engine on the target GPU with realistic lengths: time to first token, tokens per
   second per request, total throughput, and where latency breaks as concurrency grows.
4. Compute cost per million tokens at your real utilisation (GPU hours / tokens served) and compare
   with API prices, including the engineering time to run it.

Utilisation decides: a GPU busy 80% of the day can beat API prices; one busy 5% almost never does.

## Summary

- Self-host for data control, steady high volume or customisation; otherwise prefer APIs or managed
  open-model platforms.
- Memory = weights (parameters × bytes) + KV cache (context × concurrency); decoding is bandwidth
  bound, batching raises throughput.
- Quantization trades quality for memory and speed: evaluate it.
- Use an engine with continuous batching and paged attention (vLLM and similar) behind an
  OpenAI-compatible API; llama.cpp or Ollama for local use.
- Size with load tests and compare cost per token at real utilisation, including operations.

## Further reading

- Kwon et al., "Efficient Memory Management for Large Language Model Serving with PagedAttention" (vLLM, 2023): <https://arxiv.org/abs/2309.06180>
- vLLM documentation: <https://docs.vllm.ai>
- Leviathan, Kalman and Matias, "Fast Inference from Transformers via Speculative Decoding" (2022): <https://arxiv.org/abs/2211.17192>
- Frantar et al., "GPTQ: Accurate Post-Training Quantization for Generative Pre-trained Transformers" (2022): <https://arxiv.org/abs/2210.17323>
- Hugging Face, Text Generation Inference documentation: <https://huggingface.co/docs/text-generation-inference>
- llama.cpp repository: <https://github.com/ggml-org/llama.cpp>
- Ollama documentation: <https://docs.ollama.com>
