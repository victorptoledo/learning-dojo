# Inventory runbook

- Restart: `docker compose restart inventory`.
- The stock file is `/var/lib/inventory/stock.json`; back it up before an upgrade.
- Load test: `scripts/load-test.sh` against a staging server, never production.
