# ConfigMaps and Secrets

Build the image once, run it everywhere: the database URL, feature flags and passwords
belong outside it. Kubernetes stores them in **ConfigMaps** (plain configuration) and
**Secrets** (sensitive data), and hands them to containers as environment variables or files.

## ConfigMaps

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: orders-config
data:
  LOG_LEVEL: info
  PAYMENTS_URL: http://payments.billing
  application.yaml: |
    orders:
      page-size: 50
```

Three ways to use it:

```yaml
spec:
  containers:
    - name: orders
      image: ghcr.io/acme/orders:1.4.0
      env:
        - name: LOG_LEVEL                      # one key as one variable
          valueFrom:
            configMapKeyRef: { name: orders-config, key: LOG_LEVEL }
      envFrom:
        - configMapRef: { name: orders-config } # every key as a variable
      volumeMounts:
        - { name: config, mountPath: /etc/orders, readOnly: true }
  volumes:
    - name: config
      configMap:
        name: orders-config
        items: [{ key: application.yaml, path: application.yaml }]   # one file
```

A ConfigMap holds up to 1 MiB. A key referenced by `configMapKeyRef` must exist, or the Pod
does not start (`CreateContainerConfigError`), unless you add `optional: true`.

## Secrets

A Secret looks the same, with values **base64-encoded** in `data` (or plain in `stringData`,
which the API encodes for you):

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: orders-db
type: Opaque
stringData:
  DB_PASSWORD: change-me
```

Used with `secretKeyRef`, `secretRef` or a `secret` volume. Built-in types include
`kubernetes.io/tls` (a certificate and key for Ingress), `kubernetes.io/dockerconfigjson`
(registry credentials for `imagePullSecrets`) and service account tokens.

**base64 is not encryption.** What actually protects a Secret:

- **RBAC**: few people and ServiceAccounts may `get` or `list` Secrets. Anyone who can create
  a Pod in the namespace can mount any Secret there, so that permission matters too.
- **Encryption at rest**: by default etcd stores Secrets unencrypted; enable an
  `EncryptionConfiguration`, ideally with a KMS provider. Managed clusters offer it.
- **Where it lands**: Secret volumes are tmpfs, never written to the node's disk. Mount them
  only into containers that need them.
- **Out of Git**: never commit real values. Use an external manager (cloud secret stores
  through the External Secrets Operator or the Secrets Store CSI driver), or encrypt in Git
  with Sealed Secrets or SOPS.

## Updates and rollouts

| How it is consumed | Does a change reach a running Pod? |
| --- | --- |
| environment variables | **no**, only new containers see it |
| a mounted volume | **yes**, the kubelet updates the files within about a minute |
| a volume with `subPath` | **no** |

Files update atomically (through a symlink swap), so an app can watch and reload them. For
everything else, roll the Pods: `kubectl rollout restart deploy/orders`, or make the change
part of the Pod template. Kustomize's `configMapGenerator` appends a content hash to the name
(`orders-config-7g2h9k`) and Helm charts often add a `checksum/config` annotation: either way
a new config means a new template, so a normal rolling update with rollback.

Mark ConfigMaps and Secrets `immutable: true` when they never change in place: it protects
against accidents and saves the API server from watching them.

## Twelve-factor, revisited

Environment variables are simple and every language reads them. Files suit structured
configuration and certificates, and they can change without a restart. Keep secrets out of
logs and out of `env` dumps in error pages, and prefer files for them when the app allows.

## Summary

- ConfigMaps hold configuration, Secrets sensitive values; both become env vars or files.
- base64 is encoding: protect Secrets with RBAC, encryption at rest and external managers.
- Mounted files follow changes, env vars and subPath mounts do not.
- A name with a content hash or a checksum annotation turns config changes into rollouts.

## Further reading

- ConfigMaps: <https://kubernetes.io/docs/concepts/configuration/configmap/>
- Secrets: <https://kubernetes.io/docs/concepts/configuration/secret/>
- Good practices for Secrets: <https://kubernetes.io/docs/concepts/security/secrets-good-practices/>
- Encrypting data at rest: <https://kubernetes.io/docs/tasks/administer-cluster/encrypt-data/>
- External Secrets Operator: <https://external-secrets.io/latest/>
