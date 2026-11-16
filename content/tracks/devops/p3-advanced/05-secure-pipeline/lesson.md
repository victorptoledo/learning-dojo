# Secure the Pipeline

Two GitHub Actions workflows that work and are exploitable: a pull request workflow that runs
forks' code with a write token and secrets, and a release workflow with long-lived cloud keys
and a token printed in the log. Press **Enter** to create the kata folder and open it, fix
both files and save. actionlint and bats check every hardening rule.
