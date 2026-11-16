# Regex Engine

Build the engine Russ Cox describes in "Regular Expression Matching Can Be Simple And Fast": a
recursive-descent parser for patterns with groups, alternation, quantifiers, escapes and character
classes, Thompson's construction into an NFA, and a simulation that follows every possible state
at once. The hidden tests feed it the inputs that freeze Java, Python and JavaScript regexes for
minutes; yours must answer in milliseconds. Press **Enter** to create the kata folder and open
it.
