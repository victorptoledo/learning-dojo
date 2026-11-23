A release is published once; environments then **promote** it. Production must run the
exact bytes staging tested: same archives, same manifest, never a rebuild "of the same
tag". This stage adds a `.gitignore`. The deployment state lives in the repository, under
`deploy/` (a GitOps habit: every promotion is a commit):

```text
deploy/
  releases/1.2.0/      the downloaded release: archives, SHA256SUMS, manifest.json (ignored by git)
  channels/
    staging.json       which release each environment runs (committed)
    production.json
    history.log
```

**`scripts/promote.sh STORE VERSION ENV [--allow-downgrade]`**, where `ENV` is `staging` or
`production` and `STORE` is the folder above (`deploy`):

1. `STORE/releases/VERSION` must exist (`promote: no release VERSION in STORE`) and pass
   your `verify.sh` (print its report, then `promote: VERSION does not verify`), and its
   manifest's `version` must be `VERSION`;
2. the release's **digest** is `sha256:` and the SHA-256 of its `manifest.json` (which
   itself pins every archive);
3. production only: a pre-release never goes there (`promote: pre-releases never reach
   production`); staging must run `VERSION` (`promote: VERSION is not in staging`) **with
   the same digest** (`promote: VERSION changed since it reached staging`);
4. when `ENV` already runs `VERSION` with that digest, nothing changes: `promote: ENV already
   runs VERSION`, exit `0`;
5. moving to an older version is refused (`promote: ENV runs 1.10.0, newer than 1.9.0 (use
   --allow-downgrade)`), unless `--allow-downgrade` is given (a rollback). Versions compare
   by semver.org precedence: `1.10.0` > `1.9.0`, `1.3.0` > `1.3.0-rc.10` > `1.3.0-rc.2`;
6. it writes `channels/ENV.json`, under a temporary name in `channels/` then renamed, so a
   reader never sees half a file:

```json
{ "version": "1.2.0", "digest": "sha256:9c2e...", "previous": "1.1.0" }
```

   (`previous` is the version ENV ran before, or `null`), appends `ENV VERSION DIGEST` to
   `channels/history.log`, and prints `promote: ENV now runs VERSION (DIGEST)`.

Refusals print their message on stdout and exit `1`, changing nothing. A wrong number of
arguments, an unknown option or another `ENV` prints a usage line on stderr and exits `2`.

**`.github/workflows/promote.yml`**, a workflow named `promote`, started only by
`workflow_dispatch` with two required inputs: `version` (a `string`) and `environment` (a
`choice` of `staging` and `production`). Top-level `permissions: { contents: read }`,
`defaults.run.shell: bash`, and one promotion per environment at a time (a concurrency
group on `inputs.environment`, never cancelled in progress). One job, `promote`, on
`ubuntu-24.04` with a timeout, in `environment: ${{ inputs.environment }}` (so production
can require an approval), with `permissions: { contents: write }` to push the channel.
Its steps:

1. `actions/checkout`;
2. `gh release download "v$VERSION" --dir "deploy/releases/$VERSION"` with `GH_TOKEN`;
3. `scripts/promote.sh deploy "$VERSION" "$TARGET_ENV"`;
4. record it: set a `git config user.name` and `user.email`, `git add deploy/channels`,
   and **only when something is staged** (`git diff --cached --quiet` fails), `git commit`
   and `git push`.

Inputs reach the scripts through `env:` only. The tests run these steps with the fake `gh`
(it serves the releases they prepared) and a fake `git` that records each call.
