---
from: go
status: draft
concept: "Tokens and next-token prediction"
from_way: "len(s) counts bytes and utf8.RuneCountInString counts runes; a function returns deterministic values."
native_way: "A tokenizer maps text to integer ids, and the model samples the next id repeatedly; the answer streams like reading from an io.Reader."
sensei_tip: "Byte-pair encoding literally starts from bytes, so Go's byte view of strings is a good intuition: frequent byte sequences merge into one token, rare ones stay split."
---

| Go idea | LLM idea |
| --- | --- |
| `len(s)` bytes | the token count, per tokenizer |
| `rand.IntN` with weights | sampling the next token |
| `bufio.Scanner` over a stream | a streamed response |
| a slice of messages you append to | the history you resend |
| `http.MaxBytesReader` | the context window |
