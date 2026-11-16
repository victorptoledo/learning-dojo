# The Web Platform: HTML, Forms, the DOM and fetch

The browser is the runtime of half of your application, and it already knows how to do a lot:
send forms, follow redirects, keep cookies, cache responses. Frameworks build on these features;
when you know them, frameworks stop being magic. This lesson is the minimum. The Frontend and
React tracks go much further.

## HTML is a contract with the browser

Semantic elements tell the browser, search engines and screen readers what things are:

```html
<main>
  <h1>Orders</h1>
  <form method="post" action="/orders">
    <label for="qty">Quantity</label>
    <input id="qty" name="quantity" type="number" min="1" required>
    <button>Order</button>
  </form>
  <ul id="orders"></ul>
</main>
```

- A `<button>` inside a form submits it; a `<div onclick>` does not, and is invisible to keyboards.
- `<label for>` links the text to the control: clicking the label focuses the field, and screen
  readers announce it.
- `required`, `min` and `type="email"` give free client-side validation, but the **server must
  validate again**: anyone can send any request.

## Forms without JavaScript

A form posts by itself. With `method="post"`, the browser sends the fields as
`application/x-www-form-urlencoded` (`quantity=2&note=gift`), or `multipart/form-data` with
`enctype` when it has files. With `method="get"`, the fields go into the query string, which is
right for searches (`/search?q=mug`) because the result can be bookmarked and shared.

After a successful POST, the server should answer **`303 See Other`** with a `Location`: the
browser then GETs that page. This is **Post/Redirect/Get**: reloading the result page does not post
the form again. You will build it in the first checkpoint.

## The DOM and events

The **DOM** is the live tree of the page that JavaScript reads and changes:

```js
const list = document.querySelector("#orders");
const item = document.createElement("li");
item.textContent = order.note;   // text, never parsed as HTML
list.append(item);
```

Events bubble up the tree, so one listener on a parent can handle clicks on many children
(*event delegation*). `form.addEventListener("submit", e => { e.preventDefault(); ... })` takes
over a form submission to send it with `fetch` instead.

## fetch

```js
async function loadOrders() {
  const response = await fetch("/api/orders", { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new Error(`loading orders failed: ${response.status}`);
  }
  return response.json();
}
```

Three things surprise people:

1. `fetch` **does not reject on 4xx or 5xx**. It only rejects when there is no response at all
   (network down, DNS failure, CORS refusal). Check `response.ok` or `response.status`.
2. The body is a stream that can be read **once**: `await response.json()` or `.text()`, not both.
3. Sending JSON needs the header: `fetch(url, { method: "POST", headers: { "Content-Type":
   "application/json" }, body: JSON.stringify(data) })`.

A good UI has three states for every request: **loading** (disable the button, show a spinner),
**error** (a message the user can act on) and **success**.

## Never turn user text into HTML

If a comment contains `<img src=x onerror="steal()">` and you write it with
`element.innerHTML = comment`, the browser runs the attacker's code in your users' sessions. That
is **cross-site scripting (XSS)**. The fixes:

- in the DOM, use `textContent` (or a framework that escapes by default, as React does);
- when building HTML on the server, **escape** `&`, `<`, `>`, `"` and `'` in every value;
- never build HTML by concatenating user input without escaping, including attribute values.

A **Content-Security-Policy** header adds a second line of defence, but escaping is the first.

## Summary

- Use semantic HTML and real forms; they work for keyboards, screen readers and without JavaScript.
- Forms post urlencoded bodies; GET forms are for searches; answer POSTs with 303 (PRG).
- `fetch` rejects only on network failure: always check the status.
- User text goes into the page as text or escaped, never as HTML.

## Further reading

- MDN, "Your first form": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Forms/Your_first_form>
- MDN, "Using the Fetch API": <https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch>
- MDN, "Introduction to the DOM": <https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model/Introduction>
- OWASP, "Cross Site Scripting Prevention Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html>
- In the dojo: the Frontend track's *Semantic HTML*, *Forms*, *The DOM and Events* and *fetch, async Code and Cancellation in the Browser*.
