# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a grounded-answer flow for a help centre. The
   retrieved documents are untrusted, so the flow packs them into a budget, cleans and escapes
   them, flags suspicious ones, and shows an answer only when its quotes really occur in the
   documents that were sent and its links point to approved domains. Then it measures itself
   on a small labelled set, by tag, with typical, edge and adversarial cases.

The kata joins the three blocks of the phase. Long context decides **what** the model sees,
injection defences decide **how** untrusted text is shown and what may come back out, and the
evaluation tells you whether the whole thing works, including against attacks.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Long Context | Where do documents go, and where does the question go? What do you do with a document that does not fit? Why ask for quotes first? |
| Prompt Injection | Why escape before wrapping? Which hidden characters matter? Why flag instead of block? What does an output link check catch that an input check cannot? |
| Evaluating Prompts | Which cases belong in the set? Why report by tag? Why must one broken case not stop the run? |
| The katas of the phase | How did the context packer decide what fits? How did the injection guard compare domains? |
