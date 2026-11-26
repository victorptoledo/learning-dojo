# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. You need both parts:

1. **The quiz** (press **Q**): eight questions over the phase. You need 7 right.
2. **The integration kata** (press **Enter**): a build planner that parses a build file into
   structs that borrow from it, groups the targets into waves, and runs the waves on a pool of
   background threads that report progress and can be cancelled.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Lifetimes | What does `'a` in `fn plan<'a>(targets: &[Target<'a>]) -> Vec<&'a str>` tie together? When does elision give up? |
| Smart Pointers | Why is the job an `Arc<dyn Fn ...>` and not a `Box` or an `Rc`? |
| Threads, Channels and Mutex | Which data can a `thread::spawn` closure borrow, and which can a `thread::scope` one borrow? What do `Send` and `Sync` promise? |
| Parallel Word Count | How did I split the work and merge the results? |

> [!TIP]
> The kata moves from borrowing to owning on purpose. Parts 1 and 2 never allocate a name; part 3
> must own everything it hands to a thread. Notice where the line is and why the compiler draws
> it there.
