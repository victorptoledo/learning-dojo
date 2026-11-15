# Order State

An order's life cycle lives in a status string that every method re-checks. You will turn it
into the State pattern the Java 25 way: a sealed interface of records, each carrying only its
own data and owning its own transitions, plus an exhaustive `switch` the compiler checks. The
visible tests pin today's behaviour and pass before you start. Press **Enter** to create the
kata folder and open it.
