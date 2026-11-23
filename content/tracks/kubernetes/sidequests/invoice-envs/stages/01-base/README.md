The team's plain manifests are in `base/`: a Deployment and a Service, with the settings and
even the database password written into the Deployment. Make `base/` a Kustomize base.

1. `base/kustomization.yaml` lists the Deployment and the Service as `resources`.
2. **Common labels.** Every object, its Pod template and its selectors carry
   `app.kubernetes.io/name: invoices` and `app.kubernetes.io/part-of: billing`. Set them
   once in the kustomization (`labels:` with `includeSelectors: true`), not in each file.
3. **Settings.** Move `LOG_LEVEL=info`, `CURRENCY=EUR` and
   `PUBLIC_URL=http://localhost:8080` to a file `base/config.env`, and generate the
   ConfigMap `invoices-config` from it (`configMapGenerator` with `envs`). The container
   loads every key with `envFrom`; none of the three stays as a literal `env` value.
4. **The tax rates file.** `base/rates.yaml` is read by the app from
   `/etc/invoices/rates.yaml`. Generate the ConfigMap `invoices-rates` from that file
   (`files`), and mount it read-only at `/etc/invoices` as a volume.
5. **The password.** It does not belong in the base at all. The container reads
   `DB_PASSWORD` from the key `password` of a Secret `invoices-db` (`secretKeyRef`); each
   environment provides that Secret in stage 2.

Generated ConfigMaps get a hash of their content in their name (`invoices-config-7f9c...`),
and Kustomize rewrites every reference to them. Change a value, and the name changes, the
Pod template changes, and the Deployment rolls out the new settings. Without the hash, the
Pods would keep the old values until something restarts them.

The dojo builds the base (`kustomize build base`) and checks it with kubeconform.

> [!TIP]
> Run `kustomize build base` after each step. `kustomize edit add resource ...` and
> `kustomize edit add configmap ...` write the kustomization for you, if you prefer.
