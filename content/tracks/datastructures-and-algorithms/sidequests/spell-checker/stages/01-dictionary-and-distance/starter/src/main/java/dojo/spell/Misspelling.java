package dojo.spell;

/** A word of the text that is not in the dictionary, as written, and where it starts. */
public record Misspelling(String word, int offset) {
}
