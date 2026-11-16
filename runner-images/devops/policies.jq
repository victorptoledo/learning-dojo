# Reusable policies over one GitHub Actions workflow (the input), for `policy NAME POLICY`
# in devops.bash. Each returns an array of violation messages; [] means it passes.

# Every step and reusable workflow call, with where it is: {where, uses}.
def all_uses:
  [ .jobs // {} | to_entries[]
    | .key as $job
    | (if .value.uses then { where: "job \($job)", uses: .value.uses } else empty end),
      (.value.steps // [] | to_entries[] | select(.value.uses)
        | { where: "job \($job), step \(.value.name // (.key + 1 | tostring))", uses: .value.uses }) ];

# Third-party actions and reusable workflows are pinned to a full commit SHA
# (local ./ actions and docker:// images are not checked here).
def pinned_actions:
  [ all_uses[]
    | select(.uses | startswith("./") or startswith("docker://") | not)
    | select(.uses | test("@[0-9a-f]{40}$") | not)
    | "todo: \(.where): pin \(.uses) to a full 40-character commit SHA (keep the version in a comment)" ];

# The workflow sets `permissions` at the top level, and never write-all.
def explicit_permissions:
  [ if has("permissions") | not then "todo: set top-level permissions (for example `permissions: { contents: read }`)" else empty end,
    (.permissions | select(. == "write-all") | "todo: the top-level permissions are write-all"),
    (.jobs // {} | to_entries[] | select(.value.permissions == "write-all") | "todo: job \(.key) has permissions write-all") ];

# Every job that runs steps has a timeout.
def timeouts:
  [ .jobs // {} | to_entries[]
    | select(.value.steps and (.value["timeout-minutes"] | not))
    | "todo: job \(.key) has no timeout-minutes" ];

# No `run:` script interpolates attacker-controlled event data with ${{ }}; pass it
# through `env:` instead.
def no_untrusted_in_run:
  [ .jobs // {} | to_entries[]
    | .key as $job
    | .value.steps // [] | to_entries[]
    | select((.value.run // "") | test("\\$\\{\\{[^}]*(github\\.event\\.|github\\.head_ref|inputs\\.)"))
    | "todo: job \($job), step \(.value.name // (.key + 1 | tostring)): move the ${{ }} expression into env: and read it as a shell variable" ];
