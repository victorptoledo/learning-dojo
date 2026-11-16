# Debounced Search: Custom Hooks, Cleanup and Races

A search box that waits until the user stops typing, cancels the request it no longer needs,
and never shows the results of an old query. You build it from two custom hooks, each tested
on its own with `renderHook`. Press **Enter** to create the kata folder.
