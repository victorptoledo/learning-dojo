# Semantic HTML: Structure That Means Something

HTML is not a set of boxes to style: each element says **what** its content is. Browsers,
screen readers, search engines, reader modes and translation tools all read that meaning. A
`<div>` with a click handler looks like a button but is not one; a `<button>` is focusable,
works with Enter and Space and is announced as "button" for free.

## The document skeleton

```html
<!doctype html>            <!-- standards mode, not quirks mode -->
<html lang="en">           <!-- the language: pronunciation, hyphenation, translation -->
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Trail Jacket · Dojo Outdoor</title>   <!-- tab, bookmarks, search results -->
    <link rel="stylesheet" href="style.css">
    <script type="module" src="app.js"></script>  <!-- modules are deferred -->
  </head>
  <body>...</body>
</html>
```

Without the viewport meta, phones render at a fake 980px width and shrink the page.

## Landmarks: the map of a page

| Element | Implicit role | Use |
| --- | --- | --- |
| `<header>` (top level) | banner | Site brand, main navigation |
| `<nav>` | navigation | A block of major links; label several with `aria-label` |
| `<main>` | main | The unique main content; one per page |
| `<aside>` | complementary | Related but separate content |
| `<footer>` (top level) | contentinfo | Copyright, contacts, secondary links |
| `<section>` with a heading | region (when named) | A thematic group with its own heading |
| `<article>` | article | Content that stands alone: a post, a comment, a product card |
| `<form>` with a name | form | A form |

Screen-reader users jump between landmarks the way sighted users scan a page.

## Headings are an outline

One `<h1>` for the page's topic, then `<h2>` for its sections, `<h3>` inside those, and so on.
Never skip a level to get a smaller font: choose the level by **structure** and the size with
CSS. Many screen-reader users navigate by headings first.

## Text-level meaning

| Need | Use | Not |
| --- | --- | --- |
| Emphasis that changes meaning | `<em>` | `<i>` for emphasis |
| Strong importance | `<strong>` | `<b>` for importance |
| A list | `<ul>`/`<ol>` + `<li>` | lines with `<br>` |
| Terms and their descriptions | `<dl>`, `<dt>`, `<dd>` | a two-column `div` grid |
| A machine-readable value | `<time datetime="2026-12-03">`, `<data value="49.90">` | text only |
| Code | `<code>`, `<pre>`, `<kbd>` | a monospace span |
| A quotation | `<blockquote cite>`, `<q>` | italic text |
| Tabular data | `<table>` with `<caption>`, `<th scope>` | a grid of divs |

## Links and buttons

- `<a href>` **navigates** (to a page, a fragment, `mailto:`). It can be opened in a new tab,
  copied, bookmarked.
- `<button>` **acts** (submit, toggle, open a dialog). Inside a form, a button without
  `type="button"` is a **submit** button.

Link text must make sense alone: "Read the 2026 report", never "click here".

## Images and media

```html
<img src="jacket.webp" alt="Green trail jacket, front view" width="800" height="800">
<img src="divider.svg" alt="">                      <!-- decorative: empty alt -->
<figure>
  <img src="chart.png" alt="Sales doubled between May and June">
  <figcaption>Monthly sales, 2026</figcaption>
</figure>
<picture>
  <source srcset="hero.avif" type="image/avif">
  <img src="hero.jpg" alt="..." width="1200" height="600">
</picture>
```

`alt` describes what the image **conveys** in its context. Decorative images get `alt=""` so
assistive technology skips them. `width` and `height` let the browser reserve space (Phase 3).

## Validity matters

Browsers forgive broken HTML, but each one repairs it its own way: unclosed elements, a `<div>`
inside a `<p>` or duplicate `id`s produce a DOM you did not write. A validator (the W3C Nu
checker, or html-validate as in the dojo) catches them.

## Summary

- Choose elements by meaning; the styling comes from CSS.
- Landmarks (`header`, `nav`, `main`, `aside`, `footer`) and a heading outline make a page
  navigable.
- `<a>` navigates, `<button>` acts; text-level and media elements carry meaning machines use.
- Write valid HTML; `lang`, `title`, charset and viewport are part of every page.

## Further reading

- WHATWG HTML Living Standard, "Sections": <https://html.spec.whatwg.org/multipage/sections.html>
- MDN, "HTML elements reference": <https://developer.mozilla.org/en-US/docs/Web/HTML/Element>
- MDN, "Structuring documents": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Structuring_content/Structuring_documents>
- W3C WAI, "Page Structure Tutorial": <https://www.w3.org/WAI/tutorials/page-structure/>
- W3C, "HTML Accessibility API Mappings" (implicit roles): <https://www.w3.org/TR/html-aam-1.0/>
- W3C Markup Validation Service: <https://validator.w3.org/nu/>
