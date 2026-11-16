# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a prompt-chain runner. Each step renders a
   template with the previous output escaped, calls the model, checks the answer with its
   parser, retries with the error, and the whole chain stays within a token budget and records
   a trace. The tests drive it with recorded answers instead of a real model.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Decomposition and Chaining | Where are the gates? Why escape the hand-off? Why do errors compound? |
| Reasoning Prompts | Why reason first? How do you extract the answer and hide the reasoning? |
| Prompts for Tools | What makes a description good? What does a useful error result say? Where are permissions checked? |
| The katas of the phase | How did the answer extractor fall back? How did the tool checker report problems? |
