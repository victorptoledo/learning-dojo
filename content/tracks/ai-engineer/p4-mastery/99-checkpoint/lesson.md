# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards the
   Advanced and Mastery phases. You need 7 right.
2. **The integration kata** (press **Enter**): a model migration. Size an open-weight candidate
   on a GPU (weights, KV cache, how many requests fit, what a million tokens cost at your real
   utilisation), gate both candidates on the offline evals (a pinned model id, safety, quality
   overall **and per slice**, latency and cost), and run the winner as a staged canary with stable
   user buckets and automatic rollback.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Fine-Tuning Trade-offs | Which rung of the ladder fixes this problem? How do you know a tuned or distilled model is good enough? |
| Serving Open-Weight Models | What fills GPU memory besides the weights? Why does utilisation decide the price? |
| AI Features in Production | Why is a model upgrade a migration? What do you watch during a rollout, and what triggers a rollback? |
| Evaluation (Intermediate) | What can an average hide? Why do slices need a minimum size? |
| Cost and Latency, Observability (Advanced) | Which numbers do you compare, and where do they come from? |

The final exam asks you to turn the track's judgement into code that a team can rely on: every
migration is decided by the same rules, written down, tested and explainable to whoever asks why
a model was blocked.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
