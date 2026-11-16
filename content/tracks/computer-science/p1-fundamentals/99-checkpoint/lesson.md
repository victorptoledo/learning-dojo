# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a toy 16-bit computer. You decode instruction
   words with signed immediates, execute a small load-store instruction set with wrap-around
   arithmetic and branches, and count cycles, with a direct-mapped cache pricing every load and
   store and a penalty for every taken branch.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Data Representation | How do you sign-extend a 6-bit field? What is 0x7FFF + 1 in 16-bit two's complement? |
| Bit Decoder (kata) | How do shifts and masks pull a field out of a word? |
| Boolean Logic, Gates and Circuits | How does one adder add and subtract? What limits the clock? |
| The CPU | What are fetch, decode and execute? Why does a taken branch cost extra in a pipeline? |
| Memory Hierarchy | How does an address map to a cache slot? What is a conflict miss? |
| Cache Simulator (kata) | How do tag, index and offset come out of an address? |
| Processes, Threads and CPU Scheduling | What does a context switch cost? Which policy minimises waiting time? |
| CPU Scheduler (kata) | How does a simulation advance its clock? |

> [!TIP]
> Decode the whole program into `Instruction` objects first and execute those: that is what real
> CPUs do with a decoded-instruction cache, and it keeps `run` readable. Then write a small loop by
> hand (sum three words of memory, say), predict its cycle count on paper, and compare.
