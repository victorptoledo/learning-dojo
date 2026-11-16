# JSX and Function Components

A React component is a **function that returns JSX**. JSX looks like HTML, but it is
JavaScript: every tag becomes a call that creates a **React element**, a small plain object
that says "a `<button>` with these props and these children". React compares the elements
you return with the previous ones and updates the DOM.

```tsx
export function Greeting() {
  return <h1 className="title">Hello, dojo!</h1>;
}
// compiles to: jsx("h1", { className: "title", children: "Hello, dojo!" })
```

## The rules of JSX

1. **One root.** A component returns one element. Wrap siblings in a `<div>` or, when you do
   not want an extra node, a **fragment**: `<>...</>`.
2. **Close every tag.** `<img />`, `<br />`, `<input />`.
3. **camelCase attributes.** JSX attributes are JavaScript object keys: `className` instead
   of `class`, `htmlFor` instead of `for`, `onClick`, `tabIndex`. `aria-*` and `data-*` stay
   as they are.
4. **Curly braces open JavaScript.** `{expression}` inside text or attribute values:

```tsx
const user = { name: "Ada", avatar: "/ada.png" };
const items = 3;

<>
  <img src={user.avatar} alt={`Photo of ${user.name}`} />
  <p>{user.name} has {items * 2} points</p>
  <div style={{ padding: 8, color: "teal" }} />   {/* an object inside braces */}
</>
```

Only **expressions** go in braces, not statements: no `if` or `for` inside JSX. Use the
ternary `a ? b : c`, `cond && <X />` and `array.map(...)` instead (next lessons).

JSX **escapes** text: `{"<script>"}` shows the characters, it does not run them. That is why
React apps are safe from most injection by default. `dangerouslySetInnerHTML` turns that off,
and its name is a warning.

What renders: strings and numbers show; `null`, `undefined`, `true` and `false` render
nothing; arrays render each item. A plain object cannot be rendered (React throws).

## Components

A component is a function whose name starts with a **capital letter** (lowercase tags are
HTML elements). You use it like a tag:

```tsx
function Avatar() {
  return <img className="avatar" src="/ada.png" alt="Ada" />;
}

export function Profile() {
  return (
    <section>
      <Avatar />
      <h2>Ada Lovelace</h2>
    </section>
  );
}
```

Define components at the **top level** of a module, never inside another component: a
component defined inside another is a new function on every render, so React throws its state
away each time.

## Props: the inputs of a component

Data flows in through **props**, one object argument. In TypeScript, give props a type and
destructure them, with defaults where it makes sense:

```tsx
type AvatarProps = {
  name: string;
  src: string;
  size?: number;
};

function Avatar({ name, src, size = 48 }: AvatarProps) {
  return <img src={src} alt={name} width={size} height={size} />;
}

<Avatar name="Ada" src="/ada.png" />
```

Props are **read-only**: a component never changes its own props. If a value must change
over time, that is state (next lesson).

## Composition with children

Whatever you put between the tags of a component arrives as the `children` prop. This is how
you build wrappers such as cards, layouts and dialogs:

```tsx
import type { ReactNode } from "react";

function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="card">
      <h3>{title}</h3>
      {children}
    </div>
  );
}

<Card title="Profile">
  <Avatar name="Ada" src="/ada.png" />
  <p>Mathematician</p>
</Card>
```

`ReactNode` is the type of "anything React can render": elements, strings, numbers, `null`,
arrays of those. Prefer composition (passing children or elements as props) to deep
inheritance: React has no component inheritance at all.

## Components must be pure

React may call your component **many times**: on every re-render, twice in StrictMode, and
sometimes start a render and throw it away (concurrent rendering). So rendering must be a
**pure calculation**: same props and state in, same JSX out, and nothing else changed.

```tsx
let renders = 0;
function Bad({ name }: { name: string }) {
  renders++;                 // ✗ changes a variable outside the render
  document.title = name;     // ✗ touches the DOM during render
  return <h1>{name}</h1>;
}
```

Side effects belong in **event handlers** (a click) or, when they synchronise with something
outside React, in an **effect** (`useEffect`, later in this phase). Creating and changing
local variables *inside* the render is fine: they are new on every call.

## Key points

- JSX is JavaScript: tags become element objects, braces hold expressions.
- One root (or a fragment), closed tags, `className`, camelCase attributes.
- Components are capitalised functions at the top level of a module; props are their typed,
  read-only input; `children` enables composition.
- Rendering must be pure: no side effects, no changes to values outside the component.

## Further reading

- react.dev, "Describing the UI": <https://react.dev/learn/describing-the-ui>
- react.dev, "Writing Markup with JSX": <https://react.dev/learn/writing-markup-with-jsx>
- react.dev, "Passing Props to a Component": <https://react.dev/learn/passing-props-to-a-component>
- react.dev, "Keeping Components Pure": <https://react.dev/learn/keeping-components-pure>
- react.dev, "Using TypeScript": <https://react.dev/learn/typescript>
- TypeScript Handbook, "JSX": <https://www.typescriptlang.org/docs/handbook/jsx.html>
