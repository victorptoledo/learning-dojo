# Secrets Hygiene

The service needs an API token. It reads it from the file `/run/secrets/api_token` (or the
path in `API_TOKEN_FILE`) and logs `"status":"configured"` or `"status":"missing"` when it
starts; it runs either way.

The starter works, but the token (`dojo-fake-token-7f3a91`, in `.env` and
`secrets/api_token.txt`) ends up in the image:

- as a build argument and an environment variable (`docker image inspect`, `docker history`);
- as files: `COPY . .` sends `.env` and `secrets/` to the builder, and the final stage copies
  `/src` and the token file;
- and `compose.yaml` passes it as an environment variable from `.env`, where `docker inspect`
  on the running container shows it.

Fix both files:

| Rule | How it is checked |
| --- | --- |
| no `API_TOKEN` variable in the image | `Config.Env` |
| the token appears in no build step | `docker history` |
| no `.env`, `secrets/`, `api_token*` file or `/src` in the image | the files in the image |
| `.dockerignore` lists `.env` and `secrets/` | the file |
| the image still runs as a non-root user and starts without a token | `Config.User`, the container state and logs |
| Compose gives the token to `api` as the secret `api_token` from `./secrets/api_token.txt` | `secrets` in `docker compose config` |
| the token appears nowhere in the normalised compose file (no `environment`, no `env_file`) | `docker compose config` |

There are 10 tests: 1 in `tests/visible.bats` and 9 hidden ones, plus hadolint and
`compose config`.
