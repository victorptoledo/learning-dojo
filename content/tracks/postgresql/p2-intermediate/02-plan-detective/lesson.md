# Plan Detective: Fixing Row Estimates

The planner chooses well when it knows how many rows each step returns. In this kata you fix an
estimate that is wrong because two columns are related, read the statistics the planner keeps
for a column, and search captured plans for the node where the estimate went furthest from
reality. Press **Enter** to create the kata folder and open it.
