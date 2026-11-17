# Sidequest: The slow pipeline made fast

The nightly orders job in `pipeline.py` gives the right answers and takes all night. This
sidequest is the opposite of the others: the code is already there, and correct. Your job is
to make it fast **without changing a single result**.

Work like a performance engineer, every stage:

1. **Measure first.** Build a big input (the stage README says how big) and time the function
   with `time.perf_counter`, or profile it with `python -m cProfile -s cumtime`. Write down
   the number.
2. **Find the bottleneck** and say why it is slow, in big-O terms.
3. **Fix that one thing**, keeping the function's signature and results.
4. **Prove it:** the contract tests check the results against the slow version's on many
   inputs, then run your version on a large input within a time (or memory) budget the slow
   version cannot meet. Keep your measurements in `NOTES.md`: before, after, and why; the AI
   review (`[A]`) can check your reasoning.

Each stage names one function; the functions of the earlier stages stay fast. Standard
library only.
