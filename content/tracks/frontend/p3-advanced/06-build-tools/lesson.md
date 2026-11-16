# ES Modules, Bundlers and Production Builds

The katas so far load plain files: `index.html`, `style.css`, `app.js`. Real projects have
hundreds of modules, npm dependencies, TypeScript, JSX, images and environment-specific
settings. **Build tools** turn that source into a few small, cacheable files for the browser,
and give you a fast development server while you work.

## ES modules: the native format

```js
// cart.js
export function total(items) { /* ... */ }
export default class Cart { /* ... */ }

// app.js
import Cart, { total } from "./cart.js";       // static: known before running
const { format } = await import("./money.js");  // dynamic: loaded on demand
```

- Browsers load modules natively with `<script type="module">`: deferred, strict mode, their
  own scope, CORS-checked.
- Bare specifiers (`import { z } from "zod"`) do not work in browsers without an **import
  map** (`<script type="importmap">`) or a bundler that resolves them from `node_modules`.
- Static `import` lets tools see the whole dependency graph; dynamic `import()` creates split
  points.

## Why bundle at all?

HTTP/2 can load many files, but deep import chains still cost round trips (the browser learns
about `c.js` only after `b.js` arrived), npm packages ship in many formats, and you want
TypeScript, JSX, minification and hashed file names. A bundler:

1. **Resolves** the module graph from your entry points, including `node_modules`.
2. **Transforms** each module: TypeScript and JSX to JavaScript (esbuild, SWC, Babel), modern
   syntax down to your browser targets, CSS through PostCSS or Lightning CSS.
3. **Tree-shakes**: drops exports nothing imports. It needs ES modules and code without side
   effects at import time (`"sideEffects": false` in package.json helps).
4. **Code-splits**: each dynamic `import()` becomes a separate chunk loaded on demand; shared
   dependencies go in common chunks.
5. **Minifies** and writes files with **content hashes** (`app.3f9a1c.js`) so they can be
   cached forever: a change creates a new name.
6. Emits **source maps** so DevTools and error trackers show your original code.

## The tools in 2026

| Tool | Role |
| --- | --- |
| **Vite** | Dev server with native ES modules and instant hot module replacement; production builds with Rolldown (a Rust bundler with Rollup's API) |
| **esbuild**, **SWC**, **Oxc** | Very fast transformers and minifiers written in Go and Rust, used inside other tools |
| **Rollup** / **Rolldown** | Library and app bundling with excellent tree shaking |
| **webpack** / **Rspack** | The long-time standard with a huge plugin ecosystem / its Rust-based compatible successor |
| **Turbopack** | Next.js's bundler |
| Framework CLIs | Next.js, Nuxt, SvelteKit, Astro, Angular CLI: build pipelines with routing and rendering modes |

Most new projects start with Vite (or a framework built on it):

```bash
npm create vite@latest my-app
npm run dev      # dev server with HMR on http://localhost:5173
npm run build    # optimized files in dist/
npm run preview  # serve dist/ to check the production build
```

## Development vs production

| Development | Production |
| --- | --- |
| Unbundled modules served on demand, HMR keeps state on save | Bundled, split, minified, hashed |
| Full source maps, readable errors | Hidden or uploaded source maps |
| `import.meta.env.DEV === true` | Dead branches for development removed |
| Speed of feedback | Speed for the user |

**Environment variables** are replaced at build time (`import.meta.env.VITE_API_URL`):
everything in the bundle is public. Never put secrets in frontend environment variables.

## Watching what you ship

- Look at the bundle: `vite build` prints chunk sizes; tools such as rollup-plugin-visualizer
  or source-map-explorer show what is inside.
- Measure **compressed** sizes (gzip or Brotli): that is what crosses the network. JavaScript
  also costs parse and compile time on slow phones, byte for byte more than images.
- Set a budget, and question every dependency: a 70 KB date library for one format call is a
  `Intl.DateTimeFormat` away.
- Target modern browsers (Baseline) and skip transpiling to ES5; ship polyfills only where
  needed.

## Caching and deployment

- HTML: `Cache-Control: no-cache` (revalidate every time) so users get new asset names.
- Hashed assets: `Cache-Control: public, max-age=31536000, immutable`.
- Serve from a CDN, with Brotli compression and HTTP/2 or HTTP/3.
- Deploy atomically: upload new assets first, then switch the HTML, so no page references a
  file that is not there yet.

## Summary

- ES modules are the native format; bundlers add resolution of packages, transforms, tree
  shaking, code splitting, minification and hashed names.
- Vite gives a fast unbundled dev server and an optimized production build.
- Dynamic `import()` is your code-splitting tool; side-effect-free ES modules make tree
  shaking work.
- Build-time environment variables are public; never ship secrets.
- Measure compressed bundle size, cache hashed files forever and HTML never.

## Further reading

- MDN, "JavaScript modules": <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules>
- MDN, `<script type="importmap">`: <https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/script/type/importmap>
- Vite documentation, "Why Vite": <https://vite.dev/guide/why>
- Vite documentation, "Building for Production": <https://vite.dev/guide/build>
- web.dev, "Reduce JavaScript payloads with code splitting": <https://web.dev/articles/reduce-javascript-payloads-with-code-splitting>
- web.dev, "Love your cache": <https://web.dev/articles/love-your-cache>
- MDN, "Cache-Control": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Cache-Control>
