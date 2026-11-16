# Server Components, Server Functions and Frameworks

Everything so far ran in the browser. React can also run on the server, and modern React
frameworks combine both. This lesson is conceptual: the dojo's katas run React in jsdom, but
you need these ideas to read and design a Next.js or React Router app.

## Rendering strategies

| Strategy | HTML is produced | Good for |
| --- | --- | --- |
| **CSR** (client-side rendering) | in the browser, after the JavaScript loads | dashboards behind a login, highly interactive apps |
| **SSR** (server-side rendering) | on the server, per request | pages that need fresh, personalised data and fast first paint |
| **SSG** (static generation) | at build time | docs, blogs, marketing pages |
| **Streaming SSR** | on the server, sent in chunks as Suspense boundaries resolve | pages with slow parts that should not block the rest |

With SSR the browser first shows the server's HTML, then downloads the JavaScript and
**hydrates**: React attaches event handlers to the existing HTML (`hydrateRoot`). The HTML the
client would render must match the server's, or React reports a **hydration mismatch** (common
causes: `Date.now()`, `Math.random()`, `window` checks during render, locale differences).

## React Server Components

**Server Components** (RSC) are components that run **only on the server**, at request or
build time. Their code is never sent to the browser. They can be `async` and read data
directly:

```tsx
// app/products/[id]/page.tsx: a Server Component (the default in Next.js's app router)
import { db } from "@/lib/db";
import { AddToCart } from "./AddToCart";

export default async function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const product = await db.product.findUnique({ where: { id } });
  if (!product) notFound();
  return (
    <article>
      <h1>{product.name}</h1>
      <Markdown source={product.description} />   {/* the markdown library stays on the server */}
      <AddToCart productId={product.id} />         {/* a Client Component */}
    </article>
  );
}
```

**Client Components** are the components you know: state, effects, event handlers, browser
APIs. A file that starts with `"use client"` marks a **boundary**: that module and everything it
imports become part of the client bundle.

```tsx
"use client";
import { useState } from "react";

export function AddToCart({ productId }: { productId: string }) {
  const [adding, setAdding] = useState(false);
  // ...
}
```

Rules of the boundary:

- Server Components can render Client Components and pass them **serialisable** props:
  strings, numbers, plain objects and arrays, dates, promises, JSX (as `children`), and server
  functions. Not ordinary functions, class instances or symbols.
- Client Components cannot import Server Components, but can receive them as `children` or
  other JSX props, which lets a client layout wrap server content.
- Server Components have no state, effects or event handlers.
- Push `"use client"` **down** to the interactive leaves, so most of the tree stays on the
  server.

The RSC output is not HTML but a serialised description of the tree (the "RSC payload") that
React on the client merges into the page; frameworks also render it to HTML for the first load.

## Server functions

A function marked with `"use server"` runs on the server but can be called from the client,
as a form action or directly. The framework turns it into an HTTP endpoint:

```tsx
// actions.ts
"use server";
import { z } from "zod";

const AddToCart = z.object({ productId: z.string().uuid(), quantity: z.number().int().min(1).max(10) });

export async function addToCart(input: unknown) {
  const session = await auth();                       // who is calling?
  if (!session) throw new Error("Not signed in");
  const { productId, quantity } = AddToCart.parse(input);   // never trust the arguments
  await db.cartItem.upsert({ ... });
  revalidatePath("/cart");                            // refresh server-rendered data
}
```

```tsx
<form action={addToCart}>...</form>                    // works even before JavaScript loads
```

Security rules: a server function is a **public endpoint**. Anyone can call it with any
arguments, so authenticate, authorise and validate inside it, every time. Do not pass secrets
or private fields to Client Components in props: everything passed across the boundary is sent
to the browser. Mark server-only modules with the `server-only` package so importing them from
client code fails the build.

## Frameworks

React's own docs recommend a framework for new apps, because RSC, server functions, streaming,
routing and bundling must work together:

- **Next.js** (app router): file-system routes, layouts, Server Components by default, server
  functions, caching and revalidation, image and font optimisation.
- **React Router** (framework mode, formerly Remix): route modules with loaders and actions,
  progressive enhancement of forms, SSR and pre-rendering; RSC support is arriving.
- **Others:** TanStack Start, Expo (React Native) with server components, Waku (minimal RSC).

A client-only Vite app is still a fine choice for apps behind a login with no SEO needs, or
when the backend is a separate API.

## Key points

- CSR, SSR, SSG and streaming trade first paint, freshness, cost and complexity; SSR needs
  hydration and matching output.
- Server Components run only on the server, read data directly and ship no JavaScript;
  `"use client"` marks the interactive boundary; only serialisable props cross it.
- Server functions (`"use server"`) are public endpoints: authenticate, authorise and validate
  inside them; never leak secrets through props.
- Frameworks (Next.js, React Router) integrate routing, data, RSC and bundling.

## Further reading

- react.dev, "Server Components": <https://react.dev/reference/rsc/server-components>
- react.dev, `'use client'`: <https://react.dev/reference/rsc/use-client>
- react.dev, "Server Functions" and `'use server'`: <https://react.dev/reference/rsc/server-functions>
- react.dev, `hydrateRoot`: <https://react.dev/reference/react-dom/client/hydrateRoot>
- Next.js, "Server and Client Components": <https://nextjs.org/docs/app/getting-started/server-and-client-components>
- React Router, "Rendering Strategies": <https://reactrouter.com/start/framework/rendering>
