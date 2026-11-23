`rollout.yaml` describes a rollout (the repository has the one of `checkout`, and
`k8s/route.yaml` shows the HTTPRoute it drives):

```yaml
service: checkout
namespace: shop
route: checkout              # the HTTPRoute
stable: checkout-stable      # its two backends, Services
canary: checkout-canary
deployment: checkout-canary  # the canary's Deployment
steps:
  - { weight: 10, pause: 2m }    # send 10% to the canary, then watch it for 2 minutes
  - { weight: 50, pause: 2m }
  - { weight: 100 }              # everyone: the canary is the new stable
analysis:
  interval: 1m                   # measure the canary every minute of a pause
  query: sum(rate(...5xx...)) / sum(rate(...))   # its error ratio
  max: 0.01                      # above this, a check fails
  failure_limit: 2               # this many failed checks in a row roll back
  inconclusive_limit: 3          # this many checks without an answer in a row roll back
```

Start `rollout.sh` (bash, run as `bash rollout.sh COMMAND FILE`) with two commands.

**`validate FILE`** prints `rollout: FILE is valid` and exits `0`, or prints **every**
problem on stderr, one line each, and exits `1`:

| Problem line | When |
| --- | --- |
| `rollout: FIELD: is required` | `service`, `namespace`, `route`, `stable`, `canary` or `deployment` is missing |
| `rollout: FIELD: must be a Kubernetes name` | one of them is not lowercase letters, digits and `-`, starting and ending with a letter or digit, at most 63 characters |
| `rollout: steps: at least one step is required` | no `steps`, or an empty list |
| `rollout: steps[I].weight: must be an integer from 1 to 100` | |
| `rollout: steps[I].weight: must be greater than the previous step` | weights only go up |
| `rollout: steps: the last step must reach weight 100` | a rollout ends with everyone on the new version |
| `rollout: steps[I].pause: must be a duration like 30s, 5m or 1h` | an integer and `s`, `m` or `h`; a step without `pause` moves on at once |
| `rollout: steps[I].pause: must be a multiple of analysis.interval` | a pause is a whole number of checks |
| `rollout: analysis: is required` | |
| `rollout: analysis.interval: must be a duration like 30s, 5m or 1h` | and more than zero |
| `rollout: analysis.query: is required` | a non-empty string |
| `rollout: analysis.max: must be a number from 0 to 1` | |
| `rollout: analysis.failure_limit: must be an integer of at least 1` | |
| `rollout: analysis.inconclusive_limit: must be an integer of at least 1` | it is optional (default 3) |

`I` counts from 0, as in YAML paths. A missing file or one that is not YAML is an error too
(exit `1`).

**`plan FILE`** validates the same way, then prints what `run` will do:

```text
$ bash rollout.sh plan rollout.yaml
checkout: 3 steps, analysis every 60s (max error ratio 0.01, 2 failures roll back)
step 1/3: weight 10, pause 120s, 2 checks
step 2/3: weight 50, pause 120s, 2 checks
step 3/3: weight 100
```

A pause of one interval is `1 check`. Any other command line prints a usage line on stderr
and exits `2`.
