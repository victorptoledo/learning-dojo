# A Fitness Function Suite

One dependency rule is a start. A codebase that has to evolve for years needs a small suite of
fitness functions that runs on every build: metrics that show where coupling piles up, a check
that no package cycle creeps back in, and a way to adopt all of it on code that already breaks
the rules without stopping the team. You will write the metrics of Robert C. Martin's component
principles, a cycle detector and a **ratchet** over a baseline, then run them on a small app.
Press **Enter** to create the kata folder and open it.
