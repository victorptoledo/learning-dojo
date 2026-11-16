# The DOM and Events: Bubbling, Delegation and Safe Updates

The **DOM** (Document Object Model) is the live tree of objects the browser builds from HTML.
JavaScript reads and changes it, and the browser re-renders. Events tell your code what the
user (and the browser) did. Together they are how every interface reacts, with or without a
framework.

## Finding elements

```js
const form = document.querySelector("#new-task");        // first match or null
const items = document.querySelectorAll(".task");         // a static NodeList
const list = form.closest(".board");                       // nearest ancestor that matches
const title = item.querySelector(".task-title");           // search inside an element
```

`querySelectorAll` returns a **static** snapshot; `children` and `getElementsByClassName` are
**live** collections that change as the DOM changes.

## Changing the DOM safely

```js
const item = document.createElement("li");
item.className = "task";
item.dataset.id = String(task.id);          // data-id="7"
const title = document.createElement("span");
title.textContent = task.title;             // text, never parsed as HTML
item.append(title, button);
list.append(item);                          // also: prepend, before, after, replaceWith, remove
list.replaceChildren(...items);             // swap all children at once
```

- `textContent` sets **text**. `innerHTML` **parses HTML**: with user data it is an XSS hole
  (Phase 3). Use it only for HTML you wrote yourself, or not at all.
- Attributes vs properties: `input.value` is the current value, `input.getAttribute("value")`
  the initial one. `el.hidden = true`, `el.classList.toggle("done")`, `el.setAttribute("aria-expanded", "true")`.
- `<template>` holds inert markup to clone: `template.content.cloneNode(true)`.
- Many small changes in a row are fine: the browser renders once per frame. What is expensive
  is **reading layout** (`offsetHeight`, `getBoundingClientRect`) between writes (Phase 3).

## Events and listeners

```js
button.addEventListener("click", (event) => {
  console.log(event.type, event.target, event.currentTarget);
});
input.addEventListener("input", onInput);              // every change while typing
form.addEventListener("submit", (event) => {
  event.preventDefault();                              // no page reload
});
window.addEventListener("scroll", onScroll, { passive: true });
button.addEventListener("click", once, { once: true });
const controller = new AbortController();
document.addEventListener("keydown", onKey, { signal: controller.signal });
controller.abort();                                     // removes the listener
```

- `event.target` is the element that was hit (maybe a `<span>` inside your button);
  `event.currentTarget` is the element whose listener is running.
- `preventDefault()` cancels the browser's default action (submitting, following a link,
  checking a box). `stopPropagation()` stops the event from travelling further; you rarely need it.
- Prefer `click` over `mousedown` for activation: `click` also fires for Enter and Space on a
  button, and for taps.

## Propagation: capture, target, bubble

An event travels **down** from `window` to the target (capture phase), fires on the target,
then **bubbles up** through every ancestor. Most events bubble (`click`, `input`, `keydown`,
`submit`); a few do not (`focus`, `blur`; use `focusin`/`focusout` for bubbling versions).

```text
window → document → html → body → main.board → ul#todo → li.task → button   (capture)
                                                                    button   (target)
button → li.task → ul#todo → main.board → body → html → document → window   (bubble)
```

## Event delegation

Instead of one listener per item, put **one** listener on a stable ancestor and find the item
from `event.target`:

```js
board.addEventListener("click", (event) => {
  const button = event.target.closest("button[data-action]");
  if (!button || !board.contains(button)) return;
  const item = button.closest("li.task");
  if (button.dataset.action === "delete") item.remove();
});
```

Delegation works for items added later, uses less memory, and leaves nothing to clean up when
items are removed. `closest()` handles clicks on children of the button.

## Focus is part of the state

When an action removes or moves the focused element, move the focus deliberately: to the
item's new place, to the next item, or back to the input. Otherwise it drops to `<body>` and a
keyboard or screen-reader user loses their place.

## Custom events

Components can announce things with their own events:

```js
list.dispatchEvent(new CustomEvent("task:added", { detail: { id }, bubbles: true }));
document.addEventListener("task:added", (event) => console.log(event.detail.id));
```

## When the DOM is ready

`<script type="module">` and `<script defer>` run after the document is parsed, so the
elements exist. Classic scripts in `<head>` without `defer` block parsing and see an empty
body. `DOMContentLoaded` fires when the HTML is parsed; `load` waits for images and styles too.

## Summary

- Query with `querySelector`/`closest`; build with `createElement`, `textContent` and `append`.
- Never put untrusted text in `innerHTML`.
- Events capture down, hit the target and bubble up; `preventDefault` stops default actions.
- Delegate: one listener on an ancestor and `event.target.closest(...)`.
- Manage focus when the DOM changes under the user.

## Further reading

- MDN, "Introduction to the DOM": <https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model/Introduction>
- MDN, "Introduction to events": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Scripting/Events>
- MDN, "Event bubbling": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Scripting/Event_bubbling>
- MDN, `EventTarget.addEventListener()`: <https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener>
- WHATWG, DOM Standard, "Events": <https://dom.spec.whatwg.org/#events>
- web.dev, "Learn JavaScript" (the DOM and events modules): <https://web.dev/learn/javascript>
