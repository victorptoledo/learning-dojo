Three things still go wrong in a setup like this: a value written in two places drifts, a
tag is moved to a different image, and a dev setting slips into production. Close all three.

**The host is written once.** The app builds links from `PUBLIC_URL`, which today still says
`http://localhost:8080` in staging and production. In each of `overlays/staging` and
`overlays/prod`, the host appears **once** (in the patch of the Ingress rule), and
Kustomize copies it to the rest with `replacements:`:

- the component sets `PUBLIC_URL=https://invoices.example.test` (a `configMapGenerator` with
  `behavior: merge` in `components/public`), so every environment with a public endpoint
  starts from an `https://` URL;
- a replacement whose source is the Ingress rule host (`spec.rules.0.host`) writes it into
  `data.PUBLIC_URL` of `invoices-config`, replacing only the host part
  (`options: { delimiter: "/", index: 2 }`: `https://HOST` split on `/` is `https:`, ``,
  `HOST`), and into the TLS entry (`spec.tls.0.hosts.0`).

Replacements run after the overlay's patches and before the ConfigMap hash is computed, so
staging and production end up with different `invoices-config-<hash>` names. Dev keeps
`http://localhost:8080`.

**Images by digest.** A tag can be moved; a digest cannot. Staging and production run
`ghcr.io/acme/invoices:3.0.4` pinned to
`sha256:3b5a1f7e0c9d2a4b6c8e0f1a3b5c7d9e1f2a4b6c8d0e2f4a6b8c0d2e4f6a8b0c` (`digest:` next to
`newTag:` in `images:`), so staging tests exactly the bytes that production will run.

**Nothing from dev.** The staging and production builds contain no release candidate
(`-rc`), no `debug`, no `dev-only` value and no Secret.

> [!TIP]
> Check the copy: `kustomize build overlays/prod | grep -n 'acme.test'` should list the rule,
> the TLS entry and `PUBLIC_URL`, while `grep -rn acme.test overlays/prod` finds one line.
