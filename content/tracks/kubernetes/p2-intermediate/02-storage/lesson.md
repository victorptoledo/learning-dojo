# Storage: Volumes, PersistentVolumes, StorageClasses and StatefulSets

A container's file system disappears with the container. Pods that need to keep data, or to
share files between containers, mount **volumes**. Some volumes live as long as the Pod;
**persistent** volumes live on, independent of any Pod.

## Ephemeral volumes

| Volume | Lives | Use |
| --- | --- | --- |
| `emptyDir` | as long as the Pod (on the node's disk, or memory with `medium: Memory`) | scratch space, a cache, files shared between containers |
| `configMap`, `secret`, `downwardAPI`, `projected` | as long as the Pod | configuration and metadata as files |
| `ephemeral` (generic ephemeral volume) | as long as the Pod, but provisioned like a PVC | large scratch space on real disks |

A read-only root file system (Phase 3) plus an `emptyDir` for `/tmp` is a common, safe pattern.
Avoid `hostPath`: it ties a Pod to one node's file system and is a security risk.

## PersistentVolumeClaims and PersistentVolumes

The persistent storage model separates **asking** from **providing**:

- a **PersistentVolumeClaim** (PVC, namespaced) is the request: "10 GiB, ReadWriteOnce, of
  class `fast-ssd`";
- a **PersistentVolume** (PV, cluster-wide) is an actual piece of storage: a cloud disk, an
  NFS export, a local SSD;
- the control plane **binds** one claim to one volume. Pods mount the claim, not the volume.

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: uploads
spec:
  accessModes: [ReadWriteOnce]
  storageClassName: fast-ssd
  resources:
    requests: { storage: 10Gi }
---
# in the Pod spec
volumes:
  - name: uploads
    persistentVolumeClaim: { claimName: uploads }
```

**Access modes**: `ReadWriteOnce` (mounted read-write by one **node**), `ReadWriteOncePod`
(one Pod), `ReadOnlyMany`, `ReadWriteMany` (many nodes; needs a shared file system such as
NFS, EFS or Azure Files). Most block disks only support ReadWriteOnce.

## StorageClasses and dynamic provisioning

Nobody creates PVs by hand any more. A **StorageClass** names a **provisioner**, usually a
**CSI driver** (Container Storage Interface) for the cloud's disks, and its parameters:

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: fast-ssd
provisioner: ebs.csi.aws.com
parameters: { type: gp3 }
reclaimPolicy: Retain                  # Delete (the default) or Retain
volumeBindingMode: WaitForFirstConsumer
allowVolumeExpansion: true
```

When a PVC asks for that class, the driver creates a disk and a PV. `WaitForFirstConsumer`
delays that until a Pod is scheduled, so a zonal disk is created in the Pod's zone. The
**reclaim policy** decides what happens to the disk when the claim is deleted: `Delete`
removes it, `Retain` keeps it for manual recovery. One class can be marked as the cluster
default. CSI also gives **VolumeSnapshots** for backups and clones.

## StatefulSets

A Deployment's Pods are interchangeable: random names, one shared template, any order. A
database cluster needs the opposite. A **StatefulSet** gives each replica:

- a **stable name** with an ordinal: `db-0`, `db-1`, `db-2`;
- a **stable DNS name** through a headless Service: `db-0.db.data.svc.cluster.local`;
- **its own volume** from `volumeClaimTemplates`: PVCs `data-db-0`, `data-db-1`… that stay
  bound to that ordinal when the Pod is rescheduled.

```yaml
apiVersion: v1
kind: Service
metadata: { name: db }
spec:
  clusterIP: None                 # headless: per-Pod DNS records
  selector: { app: db }
  ports: [{ name: postgres, port: 5432 }]
---
apiVersion: apps/v1
kind: StatefulSet
metadata: { name: db }
spec:
  serviceName: db                 # the headless Service above
  replicas: 3
  selector: { matchLabels: { app: db } }
  template:
    metadata: { labels: { app: db } }
    spec:
      containers:
        - name: postgres
          image: postgres:18
          ports: [{ name: postgres, containerPort: 5432 }]
          volumeMounts: [{ name: data, mountPath: /var/lib/postgresql }]
  volumeClaimTemplates:
    - metadata: { name: data }
      spec:
        accessModes: [ReadWriteOnce]
        resources: { requests: { storage: 20Gi } }
```

Pods start in order (0, 1, 2), each after the previous is ready, and stop in reverse
(`podManagementPolicy: Parallel` relaxes it). Updates go from the highest ordinal down, and
`rollingUpdate.partition` holds the lower ordinals back for a staged rollout. Scaling down
**keeps** the PVCs by default (`persistentVolumeClaimRetentionPolicy` can change that), so
scaling back up finds the old data.

A StatefulSet gives identity and storage, not replication: the database itself must still
replicate, elect a leader and fail over. Operators (Phase 4) or a managed database often do
that better.

## Summary

- `emptyDir` and config volumes live with the Pod; persistent volumes outlive it.
- Pods mount PVCs; PVCs bind to PVs; StorageClasses provision PVs through CSI drivers.
- ReadWriteOnce means one node; ReadWriteMany needs a shared file system.
- `WaitForFirstConsumer` creates zonal disks where the Pod lands; reclaim policy decides
  whether data survives the claim.
- StatefulSets give ordered, stable names, DNS through a headless Service and one PVC per Pod.

## Further reading

- Volumes: <https://kubernetes.io/docs/concepts/storage/volumes/>
- Persistent volumes: <https://kubernetes.io/docs/concepts/storage/persistent-volumes/>
- Storage classes: <https://kubernetes.io/docs/concepts/storage/storage-classes/>
- StatefulSets: <https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/>
- StatefulSet API reference: <https://kubernetes.io/docs/reference/kubernetes-api/workload-resources/stateful-set-v1/>
