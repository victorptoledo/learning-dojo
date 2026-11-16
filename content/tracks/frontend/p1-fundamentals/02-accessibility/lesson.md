# Accessibility: Names, Keyboard, Focus and ARIA

About one person in six lives with a disability. Some use a screen reader, some only a
keyboard or a switch, some zoom to 400%, some cannot tell red from green, some need captions.
Accessibility (**a11y**) means they can all use what you build. It is also a legal requirement
in many places (the European Accessibility Act, the ADA, Section 508) and it helps everyone:
captions in a noisy train, keyboard shortcuts for power users.

## WCAG in one minute

The **Web Content Accessibility Guidelines** (WCAG 2.2, a W3C Recommendation) are organized
around four principles, **POUR**:

| Principle | Means | Examples |
| --- | --- | --- |
| Perceivable | Content can be seen, heard or read by some sense | Text alternatives, captions, 4.5:1 contrast |
| Operable | Everything works with the input people have | Keyboard access, no traps, enough time, 24px targets |
| Understandable | Content and behavior are predictable | Labels, clear errors, consistent navigation |
| Robust | Assistive technology can interpret it | Valid HTML, correct names, roles and states |

Each success criterion has a level: **A**, **AA** (the usual legal target) or AAA.

## The accessibility tree

The browser builds a second tree next to the DOM: for every meaningful node, a **role**
("button", "link", "heading level 2"), a **name** ("Add to cart"), a **state** (expanded,
checked, disabled) and a **value**. Screen readers read this tree, not your pixels. Open it in
DevTools (Elements → Accessibility).

## Accessible names

Every interactive element needs a name. In order of preference:

1. **Visible text content**: `<button>Save</button>`, `<a href>Pricing</a>`.
2. **A `<label>`** for form fields: `<label for="email">Email</label><input id="email">`
   (clicking the label also focuses the field). A `placeholder` is not a label: it disappears.
3. **`aria-labelledby`** pointing at visible text elsewhere.
4. **`aria-label`** when there is no visible text (an icon button):
   `<button aria-label="Close">×</button>`.

**Label in name** (WCAG 2.5.3): when an element has visible text, its accessible name must
contain it, so voice-control users can say what they see. `<button aria-label="Delete task">Remove</button>`
fails: a user saying "click Remove" gets nothing.

## Keyboard and focus

- Everything that works with a mouse must work with a keyboard: `Tab`/`Shift+Tab` move
  focus, `Enter` activates links and buttons, `Space` buttons and checkboxes, arrow keys move
  inside widgets (radio groups, menus, tabs).
- Native elements (`a[href]`, `button`, `input`, `select`, `textarea`, `summary`) are
  focusable and handle keys. A `div` is not: making one act like a button needs `tabindex`,
  a role and key handlers, and it is still worse. Use the native element.
- `tabindex="0"` adds an element to the tab order; `tabindex="-1"` makes it focusable from
  script only (useful for a `<main>` that a skip link targets). Positive values break the
  natural order: never use them.
- **Visible focus** (WCAG 2.4.7): never `outline: none` without a replacement. Use
  `:focus-visible`, which shows the ring for keyboard users and not on mouse clicks.
- **Skip links**: a first link `Skip to content` to `#main` saves keyboard users from tabbing
  through the whole navigation on every page.
- When you remove or move the focused element, put the focus somewhere sensible, or it falls
  back to `<body>` and the user is lost.

## ARIA: the last resort

ARIA attributes change the accessibility tree, nothing else: no behavior, no keyboard support.
The first rule of ARIA: **if a native element does the job, use it**. ARIA is for what HTML
cannot say:

| Need | ARIA |
| --- | --- |
| A toggle's state | `aria-expanded="true/false"` on the button, `aria-controls="panel-id"` |
| A live message (saved, 3 results, an error) | `role="status"` (polite) or `role="alert"` (assertive) |
| A loading region | `aria-busy="true"` |
| Describe a field (hint, error) | `aria-describedby="hint-id error-id"`, `aria-invalid="true"` |
| The current item of a set | `aria-current="page"` in a breadcrumb or nav |
| Hide decoration from AT | `aria-hidden="true"` (never on focusable elements) |

"No ARIA is better than bad ARIA": a wrong role or a stale `aria-expanded` misleads users.

## Color and contrast

- Text needs a contrast ratio of at least **4.5:1** against its background (3:1 for large
  text, and for icons and focus indicators against their surroundings).
- Never use color alone to convey meaning: add text or an icon to "the red fields are invalid".
- Respect user settings: text must survive 200% zoom, and `prefers-reduced-motion` asks you to
  tone down animation.

## Testing accessibility

Automated tools (axe-core, Lighthouse, WAVE) find about a third to a half of the problems:
missing names, contrast, invalid ARIA. The rest needs people: **tab through the page**, use it
with a screen reader (NVDA or Narrator on Windows, VoiceOver on macOS and iOS, TalkBack on
Android), zoom to 200%, and test with real users when you can.

## Summary

- Build on native HTML elements; they come with roles, names, states and keyboard support.
- Every control needs an accessible name that contains its visible text.
- Everything works by keyboard, with a visible focus and focus that is never lost.
- ARIA describes state and relationships that HTML cannot; it adds no behavior.
- Check contrast, do not rely on color alone, and combine tools with manual testing.

## Further reading

- W3C, Web Content Accessibility Guidelines (WCAG) 2.2: <https://www.w3.org/TR/WCAG22/>
- W3C WAI, "WCAG 2 at a Glance": <https://www.w3.org/WAI/standards-guidelines/wcag/glance/>
- W3C WAI-ARIA Authoring Practices Guide (APG): <https://www.w3.org/WAI/ARIA/apg/>
- W3C, "Using ARIA" (the rules of ARIA use): <https://www.w3.org/TR/using-aria/>
- MDN, "Accessibility": <https://developer.mozilla.org/en-US/docs/Web/Accessibility>
- web.dev, "Learn Accessibility": <https://web.dev/learn/accessibility>
- Deque, axe-core rule descriptions: <https://dequeuniversity.com/rules/axe/>
