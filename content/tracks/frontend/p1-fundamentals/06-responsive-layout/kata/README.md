# Responsive Layout: a Recipe Site in Three Sizes

`index.html` is finished and stays as it is. Write the layout in **`style.css`**. The tests
open the page at 320, 375, 768, 1280 and 1600 pixels wide and **measure** where the boxes land,
so any technique that produces the right layout passes. Work mobile first: the one-column
layout with no media query, then `@media (min-width: ...)` for wider screens.

| Width | Layout |
| --- | --- |
| Any | `box-sizing: border-box` on every element, `margin: 0` on `body`, no horizontal scrollbar even at 320px, images never wider than their card |
| Any | `.page` is a centered column at most `72rem` wide (padding included) with `1rem` of padding on the sides; header, layout and footer use it |
| Any | `.cards` is a grid with a `1rem` gap. Cards in a row are equally tall, and each card is a flex column whose `.card-link` sits at its bottom (`margin-top: auto`) |
| Any | The nav `ul` is a flex row that **wraps** |
| Below 40rem | One column of cards; the aside below main |
| From 40rem (640px) | Two equal columns of cards |
| From 64rem (1024px) | Three columns of cards; main and the aside side by side, the aside `16rem` wide on the right, `2rem` between them, both starting at the same height; brand and nav on one line |

Hints: `grid-template-columns: repeat(3, 1fr)`; for main and aside,
`grid-template-columns: minmax(0, 1fr) 16rem`. `minmax(0, 1fr)` lets the column shrink below
its content's width, which a plain `1fr` does not.

To look at the page in your own browser (resize the window, or use the device toolbar of the
DevTools):

```bash
docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
```

There are 12 tests: 1 in `tests/visible.test.js` and 11 hidden ones.
