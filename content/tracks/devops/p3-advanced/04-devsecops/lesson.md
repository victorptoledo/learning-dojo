# DevSecOps: Supply Chain, Secrets and Pipeline Security

Security used to be a gate at the end: a penetration test weeks before launch, a list of
findings, a delay. **DevSecOps** moves security into the same automated path as everything
else, where findings are cheap to fix ("shift left"), while still watching production ("shield
right"). It also recognises that the pipeline itself is a target: it holds the keys to
production.

## Security checks in the pipeline

| Check | Finds | Examples | When |
| --- | --- | --- | --- |
| **secret scanning** | keys and passwords committed to Git | gitleaks, GitHub push protection, trufflehog | pre-commit, on push |
| **SCA** (software composition analysis) | dependencies with known vulnerabilities (CVEs) and bad licences | Dependabot, Renovate, OSV-Scanner, Snyk | every PR, and daily for new CVEs |
| **SAST** (static analysis) | injection, unsafe APIs, crypto mistakes in your code | CodeQL, Semgrep | every PR |
| **image scanning** | vulnerable OS packages and libraries in images | Trivy, Grype | after build, and on images in the registry |
| **IaC scanning** | public buckets, open security groups, missing encryption | Checkov, Trivy config, tfsec | every PR touching Terraform or manifests |
| **DAST** | vulnerabilities of the running app | OWASP ZAP | against a test environment |

Make them useful, not noisy: fail the build only on what matters (a high-severity CVE with a
fix available, a verified secret), triage the rest into the backlog, and give every exception
an owner and an expiry date. A scanner everybody ignores is worse than none.

## Secrets

- **Never in Git**: a secret committed once lives in the history forever; revoke and rotate
  it, do not just delete the line.
- Store them in a **secrets manager** (Vault, AWS Secrets Manager, Azure Key Vault, Google
  Secret Manager), fetch them at run time, and audit access.
- Prefer **short-lived credentials** to stored ones: CI exchanges an **OIDC** token for a
  temporary cloud role, workloads use their cloud identity (IRSA, Workload Identity), Vault
  issues dynamic database credentials that expire.
- Scope the trust: an OIDC role trusts one repository and one branch or environment
  (`repo:acme/infra:environment:production`), never a whole organisation.

## The software supply chain

Your artefact contains code you did not write: base images, packages, actions, build tools.
Attacks such as SolarWinds, Codecov, the xz backdoor and compromised GitHub Actions went
through that chain. Defences:

- **pin** everything by digest or hash: base images (`@sha256:`), lock files with integrity
  hashes, actions by commit SHA; update through automated, reviewed pull requests;
- produce an **SBOM** (software bill of materials, in SPDX or CycloneDX, with Syft or
  BuildKit) for every image, so you can answer "are we affected by this CVE?" in minutes;
- **sign** artefacts (Sigstore Cosign, keyless with OIDC) and attach **provenance**
  attestations describing how and where they were built;
- **verify** at deploy time: an admission policy (Kyverno, Sigstore policy-controller) only
  admits images signed by your pipeline;
- the **SLSA** framework grades build integrity: from "provenance exists" up to hardened,
  isolated builders whose provenance cannot be forged.

## Hardening the pipeline

CI/CD runs code from many sources with powerful credentials. The common attacks on GitHub
Actions and their fixes:

| Attack | Fix |
| --- | --- |
| a fork's pull request runs with secrets through `pull_request_target` and a checkout of its head | use `pull_request` for untrusted code; never check out PR code in privileged workflows |
| **script injection**: `run: echo "${{ github.event.issue.title }}"` turns a title into shell code | pass untrusted values through `env:` and quote them |
| a compromised action tag | pin to commit SHAs; allow-list actions |
| a token that can do everything | `permissions:` least privilege per job |
| tokens left in `.git/config` | `persist-credentials: false` |
| `curl ... \| bash` installers | pinned, verified tools |
| self-hosted runners reused between jobs | ephemeral runners, never for public repositories |

Protect the definitions too: branch protection and code owners on `.github/workflows/`,
required reviews, and environments with required reviewers for production.

## Policy as code

Rules such as "no public buckets", "containers run as non-root", "images come from our
registry" are best written as code and enforced automatically: **OPA/Rego** (with Conftest in
CI or Gatekeeper in the cluster), **Kyverno** for Kubernetes, Checkov policies for IaC. The
same policy runs in the pull request (fast feedback) and at admission (the last line of
defence).

## Summary

- Shift left: secret scanning, SCA, SAST, image and IaC scanning in every PR, tuned to fail
  only on what matters.
- Keep secrets in a manager; prefer short-lived OIDC credentials with narrow trust.
- Pin the supply chain, generate SBOMs, sign artefacts with provenance and verify at deploy.
- Harden CI/CD: no untrusted code with secrets, no script injection, pinned actions,
  least-privilege tokens, protected workflow files.
- Enforce rules as policy as code in CI and at admission.

## Further reading

- GitHub Actions secure use reference: <https://docs.github.com/en/actions/reference/security/secure-use>
- OWASP Top 10 CI/CD Security Risks: <https://owasp.org/www-project-top-10-ci-cd-security-risks/>
- SLSA framework: <https://slsa.dev/>
- Sigstore documentation: <https://docs.sigstore.dev/>
- OpenSSF Scorecard: <https://scorecard.dev/>
