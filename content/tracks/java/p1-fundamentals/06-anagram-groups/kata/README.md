# Anagram Groups

Two words are **anagrams** when they use the same letters the same number of times:
`listen`, `silent` and `enlist`. Implement the three methods of `AnagramGroups`.

1. `key(word)`: the word's letters, lowercased and sorted. `key("Listen")` is `"eilnst"`.
   Spaces around the word are ignored.
2. `group(words)`: a map from each key to the words with that key.
   - Keys appear in the order of their **first word** in the input.
   - Inside a group, words keep their **input order**, stripped of surrounding spaces.
   - Blank words are skipped. A word that appears again (ignoring case) is kept only once,
     the first time.
   - The map and its lists are **unmodifiable**.
3. `families(words)`: only the groups with **two or more** words, each sorted
   alphabetically, the biggest family first; families of the same size are ordered by their
   first word.

```java
group(List.of("listen", "google", "Silent", "enlist", "listen"))
    == {"eilnst"=[listen, Silent, enlist], "eggloo"=[google]}

families(List.of("tea", "listen", "ate", "silent", "eat", "dojo"))
    == [[ate, eat, tea], [listen, silent]]
```

> Tip: `toCharArray()`, `Arrays.sort`, `new String(chars)`, `LinkedHashMap`,
> `computeIfAbsent`, `List.copyOf`, `Comparator.comparingInt(...).reversed().thenComparing(...)`.
