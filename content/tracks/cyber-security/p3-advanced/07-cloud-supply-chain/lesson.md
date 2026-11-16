# Cloud Security and the Software Supply Chain

Two places where modern breaches start that are not your application code: the cloud account
it runs in, and everything that goes into building it.

## Shared responsibility

The provider secures the cloud (data centres, hypervisors, the managed service's software);
**you** secure what you put in it: identities and permissions, network rules, data, your
code and configuration. The more managed the service, the more the provider owns, but
**identity, data and configuration are always yours**. Almost every cloud breach is a
customer-side misconfiguration or a stolen credential, not a provider failure.

## Identity is the perimeter

- **No long-lived keys.** Workloads use roles (instance profiles, workload identity,
  managed identities); CI uses **OIDC federation** to get short-lived credentials; humans use
  SSO with MFA. Static access keys leak through repositories, laptops and logs.
- **Least privilege.** Start from no permissions and add what the job needs; scope by
  resource (`arn:...:bucket/app-uploads/*`), not `*`. Use access analysers to find unused
  permissions and remove them.
- **Separate accounts or projects** per environment and team, so one leaked credential does
  not reach production. Guardrails at the organisation level (service control policies,
  organisation policies) block dangerous actions everywhere, such as disabling audit logs.
- **Protect the root/owner account** with hardware MFA and never use it day to day.

## The misconfigurations behind most breaches

| Misconfiguration | Control |
| --- | --- |
| a public storage bucket | block public access at the account level; review policies |
| a database or admin port open to `0.0.0.0/0` | private subnets, security groups by source, bastions or private endpoints |
| instance metadata reachable by SSRF | IMDSv2 only, hop limit 1 |
| no audit logging, or logs in the same account an attacker controls | CloudTrail / Activity Log to a separate, locked log account |
| unencrypted volumes and snapshots shared publicly | encryption by default with KMS, snapshot policies |
| secrets in environment variables of functions, readable by many | a secrets manager with per-function access |

**Cloud security posture management (CSPM)** tools (AWS Security Hub, Microsoft Defender for
Cloud, Google Security Command Center, open-source Prowler) check accounts against CIS
benchmarks continuously. **Infrastructure as code** with policy checks in CI (Checkov,
tfsec/Trivy, OPA/Conftest) catches the same mistakes before they are deployed.

## The software supply chain

Your artifact is only as trustworthy as everything that went into it. Attacks seen in
practice: malicious packages with look-alike names (*typosquatting*), hijacked maintainer
accounts pushing a bad version, a backdoor contributed over years (the 2024 `xz` utils
case), compromised build systems (SolarWinds), and leaked CI secrets.

### Dependencies

- **Lock** exact versions (`package-lock.json`, `poetry.lock`, `go.sum`, Maven with pinned
  versions) and install from the lockfile (`npm ci`, `uv sync --locked`).
- **Scan** for known vulnerabilities (OSV, Dependabot, `pip-audit`, `npm audit`,
  `govulncheck`, OWASP Dependency-Check) and update on a schedule, with tests.
- **Review** new dependencies: maintenance, popularity, permissions, install scripts. Fewer
  dependencies is a security feature.
- Pull through an internal registry or proxy, and never let a private package name resolve
  from the public registry (*dependency confusion*).

### Artifacts and provenance

- Generate an **SBOM** (Software Bill of Materials, SPDX or CycloneDX) for every build, so you
  can answer "are we affected?" in minutes when the next big CVE lands.
- **Sign** artifacts and container images (Sigstore cosign, which can sign keylessly with
  the CI's OIDC identity) and verify signatures before deploying (an admission controller).
- Record **provenance**: which source commit, which builder, which inputs. **SLSA** defines
  levels for build integrity, up to hermetic, isolated builds with signed provenance.
- Pin container base images by **digest** (`@sha256:...`), not by mutable tags.

### The pipeline

CI/CD holds the keys to production, so harden it like production: minimal token permissions
(`permissions: contents: read` in GitHub Actions), third-party actions pinned by **commit
SHA**, no secrets exposed to workflows triggered by untrusted forks, protected branches and
environments with required reviews, and ephemeral runners.

## Summary

- Shared responsibility: identity, data and configuration are always yours.
- Short-lived, least-privilege identities everywhere; separate accounts; guardrails.
- Check posture continuously and check infrastructure code before it ships.
- Supply chain: locked and scanned dependencies, SBOMs, signed artifacts with provenance (SLSA), hardened CI.

## Further reading

- CISA and NSA, *Top Ten Cloud Security Mitigation Strategies*: <https://www.cisa.gov/resources-tools/resources/nsacisa-top-ten-cloud-security-mitigation-strategies>
- AWS Well-Architected Framework, Security Pillar: <https://docs.aws.amazon.com/wellarchitected/latest/security-pillar/welcome.html>
- SLSA, Supply-chain Levels for Software Artifacts: <https://slsa.dev/>
- NIST SP 800-218, *Secure Software Development Framework (SSDF)*: <https://csrc.nist.gov/pubs/sp/800/218/final>
- OpenSSF Scorecard: <https://scorecard.dev/>
- Sigstore documentation: <https://docs.sigstore.dev/>
- GitHub Docs, *Security hardening for GitHub Actions*: <https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions>
