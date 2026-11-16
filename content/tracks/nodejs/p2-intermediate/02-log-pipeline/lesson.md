# Log Pipeline: Transform Streams and Backpressure

Filter a log too big for memory: Transform streams that split lines across chunks, parse and
validate JSON entries, filter them by level and write them back, all connected with `pipeline`
so errors propagate and backpressure works. Press **Enter** to create the kata folder and open it.
