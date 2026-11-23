The rules are generated; now make sure every rule file in the repository, generated or
hand-written, follows the same conventions, and have Prometheus load them.

**`scripts/slogen.sh lint RULES...`** checks one or more Prometheus rule files:

| Rule | Problem line |
| --- | --- |
| every alert has a `for` | `FILE: ALERT: no for` |
| its `severity` label is `page` or `ticket` | `FILE: ALERT: severity must be page or ticket` |
| it has a `summary` annotation | `FILE: ALERT: no summary` |
| its `runbook_url` annotation is an `https://` URL | `FILE: ALERT: no runbook_url` |
| a record is named `level:metric:operations` (three parts joined by `:`, letters, digits and `_`) | `FILE: RECORD: record names are level:metric:operations` |
| no two records have the same name and the same labels | `FILE: RECORD: recorded twice with the same labels` |

`FILE` is the path as given and `ALERT`/`RECORD` the rule's name. It prints every problem of
every file, one line each, in file order and, within a file, the alert problems, then the
names, then the duplicates; then `lint: failed`, and exits `1`. A file that does not exist
or is not YAML is a problem too (`FILE: no such file`, `FILE: not YAML`). With no problem it
prints `lint: ok (N rules)`, the number of rules in all the files, and exits `0`. `lint`
with no file prints the usage line on stderr and exits `2`. The generated rules must pass.

**`prometheus.yml`** at the repository root (checked with `promtool check config`):

- `global`: `scrape_interval` and `evaluation_interval` of `30s` (the alerts' `for` of 2
  minutes then means four evaluations);
- `rule_files`: a glob that loads `rules/slos.rules.yml` and any other `rules/*.rules.yml`;
- `alerting`: one Alertmanager, `alertmanager:9093`, in a `static_configs`;
- one scrape job, `kubernetes-pods`, with `kubernetes_sd_configs` for the `pod` role and
  `relabel_configs` that:
  1. **keep** only the pods annotated `prometheus.io/scrape: "true"`
     (`__meta_kubernetes_pod_annotation_prometheus_io_scrape`);
  2. take the metrics path from `prometheus.io/path` when the pod has one, into
     `__metrics_path__`;
  3. take the port from `prometheus.io/port` when there is one, replacing the port of
     `__address__` (or adding it: `10.0.0.5:80` with port `8080` gives `10.0.0.5:8080`);
  4. set `job` from the pod label `app` (`__meta_kubernetes_pod_label_app`): the SLOs
     select their metrics by `job`;
  5. copy the namespace and the pod name into the labels `namespace` and `pod`.

The tests run your `relabel_configs` through a small simulator of Prometheus relabeling
(the actions `keep`, `drop` and `replace`, with anchored regexes), on pods with and without
each annotation.
