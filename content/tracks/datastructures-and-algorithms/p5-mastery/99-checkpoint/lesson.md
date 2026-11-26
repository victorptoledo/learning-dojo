# Checkpoint 5: Final Exam 🥋

This is the **final exam** of the Data Structures & Algorithms track. It closes the **Mastery**
phase and, with it, the track. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the Mastery phase and the ideas the whole
   track leans on. You need 7 right.
2. **The integration kata** (press **Enter**): a canonical Huffman compressor, the half of
   DEFLATE that zip, gzip and PNG use to turn frequent bytes into short codes. It brings
   together a greedy algorithm and its exchange argument, a heap, a tree walked without
   recursion, a sort on two keys, and bits packed and read with shifts and masks, on 40 MB of
   data.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Greedy Algorithms | What makes the greedy choice safe here? Which exchange argument shows that merging the two lightest trees loses nothing? |
| Bit Manipulation | How do I set the top bit of a byte? Why `>>>` and `& 0xFF` with Java's signed bytes? |
| Solving Problems Out Loud | Which cases would I test by hand before running the tests: empty input, one symbol, all 256? |
| Trees, Binary Search Trees and Heaps (Intermediate) | What does the heap order by, and how do I break ties so the result is unique? |
| Sorting and Binary Search (Intermediate) | What is the comparator for "length, then value"? |

> [!TIP]
> Do not build codes as strings. A code is a number and a length: write it with
> `buffer = (buffer << length) | code` and flush whole bytes from the top of the buffer; read it
> back one bit at a time with `(bits[i >>> 3] >>> (7 - (i & 7))) & 1`.
