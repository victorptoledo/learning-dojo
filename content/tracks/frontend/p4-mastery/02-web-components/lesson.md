# Web Components, Progressive Enhancement and Frontend Architecture

Frameworks come and go; the platform stays. **Web components** are the browser's own component
model, and **progressive enhancement** is the architectural habit that keeps pages working when
JavaScript is slow, broken or blocked. This lesson connects them with the bigger choices: how
and where a frontend renders, and how teams keep large UIs consistent.

## Custom elements

```js
class CopyButton extends HTMLElement {
  static observedAttributes = ["text"];

  connectedCallback() {                       // added to the document
    this.button ??= Object.assign(document.createElement("button"), { type: "button" });
    this.button.textContent = this.getAttribute("label") ?? "Copy";
    this.button.addEventListener("click", this);
    this.append(this.button);
  }
  disconnectedCallback() {                    // removed: clean up listeners and timers
    this.button.removeEventListener("click", this);
  }
  attributeChangedCallback(name, oldValue, newValue) { /* react to attribute changes */ }
  async handleEvent() {
    await navigator.clipboard.writeText(this.getAttribute("text") ?? "");
    this.dispatchEvent(new CustomEvent("copied", { bubbles: true }));
  }
}
customElements.define("copy-button", CopyButton);   // names need a hyphen
```

```html
<copy-button text="npm create vite@latest" label="Copy the command"></copy-button>
```

They work in any framework or none, which is why design systems for many teams (GitHub,
Adobe, Microsoft, Salesforce) ship them.

## Shadow DOM and templates

```js
const template = document.createElement("template");
template.innerHTML = `<style>:host { display: block } ::slotted(h2) { margin: 0 }</style>
  <section part="panel"><slot name="title"></slot><slot></slot></section>`;

class InfoCard extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: "open" }).append(template.content.cloneNode(true));
  }
}
```

- The **shadow root** encapsulates DOM and styles: outside CSS does not leak in, inner CSS
  does not leak out. Inherited properties (color, font) and custom properties **do** cross the
  boundary, which is how themes reach inside.
- **Slots** place the user's light-DOM children; `::part()` lets outside CSS style what you
  expose.
- **Declarative shadow DOM** (`<template shadowrootmode="open">`) lets the server render it, so
  it works before JavaScript runs.
- Accessibility caveat: `aria-labelledby` and `<label for>` cannot point across shadow
  boundaries yet; keep labels and their controls in the same tree, or use
  `ElementInternals` for form-associated custom elements.

## Progressive enhancement

Build in layers: HTML that works on its own, CSS that improves it, JavaScript that enhances it.

- A `<form action method>` that submits without script, then `fetch` with inline errors.
- A list of links that becomes tabs; a `<details>` element as a no-JS disclosure.
- **Custom elements as islands**: the server sends `<copy-button>` with a plain fallback
  inside; the script upgrades it when it loads. Unsupported or failed scripts leave the
  fallback.
- Use native features that need no script: `<dialog>` with `command`/`commandfor`, the
  `popover` attribute, `<details name>` accordions, CSS `:has()` and scroll-driven animations.

## Where to render

| Strategy | HTML produced | Strengths | Costs |
| --- | --- | --- | --- |
| **Static generation** (SSG) | At build time | Fastest TTFB and LCP, cheap to host on a CDN | Rebuild to change content |
| **Server-side rendering** (SSR) | Per request | Fresh, personalized, good LCP | Server cost; hydration cost on the client |
| **Client-side rendering** (SPA) | In the browser | App-like navigation, simple hosting | Slow first load, LCP and SEO suffer, more JavaScript |
| **Islands / partial hydration** | Server HTML with interactive parts | Little JavaScript, fast | More complex tooling (Astro, Fresh, Eleventy with web components) |
| **Streaming SSR and server components** | Server HTML streamed in parts | Fast first bytes, less client code | Framework-specific mental model |

Choose per page: a marketing page and a documentation site are static; a dashboard can be
client-rendered behind a login; a store mixes SSR for product pages and islands for the cart.

## Architecture for large frontends

- **Design systems**: tokens (custom properties), accessible components, documentation and
  usage rules in one versioned package; this is what keeps hundreds of screens consistent.
- **Boundaries**: organize by feature, not by file type; keep data access, state and view
  concerns apart; share through explicit modules.
- **Micro-frontends** (independent apps per team, composed at runtime or build time) solve
  organizational scaling at a real performance and consistency cost; most teams are better
  served by a modular monolith with clear ownership.
- **Decide with evidence**: Core Web Vitals, bundle budgets, accessibility audits and change
  failure rates, not fashion.

## Summary

- Custom elements give framework-independent components with lifecycle callbacks.
- Shadow DOM encapsulates DOM and styles; slots, parts and custom properties connect them to
  the page; watch cross-boundary ARIA references.
- Progressive enhancement: HTML first, then CSS, then JavaScript, with fallbacks.
- Choose SSG, SSR, CSR or islands per page by what it needs.
- Design systems and clear module boundaries scale teams better than more frameworks.

## Further reading

- MDN, "Web Components": <https://developer.mozilla.org/en-US/docs/Web/API/Web_components>
- MDN, "Using custom elements": <https://developer.mozilla.org/en-US/docs/Web/API/Web_components/Using_custom_elements>
- MDN, "Using shadow DOM": <https://developer.mozilla.org/en-US/docs/Web/API/Web_components/Using_shadow_DOM>
- WHATWG HTML, "Custom elements": <https://html.spec.whatwg.org/multipage/custom-elements.html>
- web.dev, "Rendering on the Web": <https://web.dev/articles/rendering-on-the-web>
- MDN, "Progressive enhancement" (glossary): <https://developer.mozilla.org/en-US/docs/Glossary/Progressive_Enhancement>
- MDN, "Popover API": <https://developer.mozilla.org/en-US/docs/Web/API/Popover_API>
