# Parallel Fetch

Many slow calls at once, one virtual thread each: keep the results in the order of the calls,
turn failures and timeouts into values of a sealed type, and make sure one stuck call never
holds the others. Press **Enter** to create the kata folder and open it.
