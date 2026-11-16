# Checkpoint 1: a Product Page

Build the product page of an outdoor shop from an empty `<body>`: semantic HTML, a form,
accessible names, a responsive layout, design tokens and a dark theme. No JavaScript.

## `index.html`

- A `<header>` with a brand link and `<nav aria-label="Main">` holding a list of at least three
  links; a `<main>`; a `<footer>`.
- In main, a breadcrumb: `<nav aria-label="Breadcrumb">` with an `<ol>` of three links (Home,
  Jackets, Trail Jacket); the last one has `aria-current="page"`.
- An `<article class="product">` with:
  - `<img class="product-photo" src="product.svg" ...>` with a real `alt` and
    `width="800" height="800"`;
  - a `<div class="product-details">` holding the `<h1>Trail Jacket</h1>`, the price
    `<p class="price"><data value="49.90">€49.90</data></p>`, a short description and a
    `<dl class="specs">` with at least three `<dt>`/`<dd>` pairs;
  - a `<form class="buy">` with a `<fieldset>`/`<legend>Size</legend>` of four radio buttons
    `name="size"` labelled S, M, L and XL (choosing one is `required`), a "Quantity" field
    (`type="number"`, 1 to 10, starting at 1) and `<button type="submit">Add to cart</button>`.
- Valid HTML and no axe-core problems.

## `style.css`

- Custom properties on `:root`: at least `--color-bg`, `--color-text` and `--color-accent`.
  The page uses them; the button's background is `var(--color-accent)`.
- A dark theme with `@media (prefers-color-scheme: dark)` that redefines the tokens: a dark
  background, light text, and enough contrast everywhere in both themes.
- Below `56rem` the photo is above the details and as wide as the product; from `56rem` they
  sit side by side in two equal columns, starting at the same height.
- The four sizes are in one row on a desktop.
- A visible focus ring (`:focus-visible`), and no horizontal scrollbar on a 375px phone.

To look at the page in your own browser (switch the theme in the DevTools' Rendering panel,
"Emulate CSS media feature prefers-color-scheme"):

```bash
docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
```

There are 15 tests: 1 in `tests/visible.test.js` and 14 hidden ones.
