# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): an issue tracker whose issues live in an
   external store. You connect it with `useSyncExternalStore`, keep the filter in the address
   bar, key rows and the editor so state stays with the right issue, meet a render budget, and
   keep the page usable with a keyboard and a screen reader.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Reconciliation: Render, Commit and Fiber | When does React keep state and when does it reset it? What does a key tell it? |
| Performance: memo, useMemo, useCallback and the React Compiler | Why did this component render? Which prop broke the shallow comparison? |
| Render Budget (kata) | Which state could move down so fewer components render? |
| State Management at Scale | Is this server, URL, form, local or shared client state? What does a selector save you? |
| Testing with Testing Library and Accessible Components | Would a user find this element by its role and name? Where does the focus go? |
| Accessible Tabs (kata) | Which element owns the keyboard, and how do you move the focus? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
