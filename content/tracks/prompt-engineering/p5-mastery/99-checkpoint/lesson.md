# Checkpoint 5: Final Exam 🥋

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you need
both:

1. **The quiz** (press **Q**): eight questions over the mastery phase. You need 7 right.
2. **The integration kata** (press **Enter**): a prompt release. A versioned prompt file pins
   the template, the model snapshot, the parameters and the output contract; a release check
   makes the version number tell the truth about what changed; a stable share of users goes to
   the candidate; every call is guarded with a retry for format problems and a fallback
   version; and the call records decide whether the candidate is promoted or rolled back.

The kata brings the track together. The template escapes its input (Fundamentals), the output
contract is JSON with required keys (Structured Output), the retry sends the error back
(Intermediate), and the release, rollout and fallback come from the Mastery lessons.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| The Eval Harness kata | Why compare versions case by case? What blocks a release? |
| Versioning Prompts | What does a version pin? Which changes are major, minor or patch? Why a dated snapshot and not an alias? |
| Prompts in Production | Which failures are worth a retry and which are not? What does a fallback need? Which signals decide a rollback? |
