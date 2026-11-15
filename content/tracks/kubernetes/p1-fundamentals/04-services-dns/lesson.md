# Services, EndpointSlices and Cluster DNS

Pods come and go, and each new Pod gets a new IP. Clients need something that stays put. A
**Service** is a stable name and virtual IP in front of a changing set of Pods, chosen by a
label selector, with load balancing built in.

## A ClusterIP Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: orders
spec:
  type: ClusterIP              # the default: reachable inside the cluster only
  selector:
    app: orders                # every ready Pod with this label is a backend
  ports:
    - name: http
      port: 80                 # what clients connect to: orders:80
      targetPort: http         # the container port, by name or number (8080)
      protocol: TCP
```

Three numbers are easy to mix up:

| Field | Where | Meaning |
| --- | --- | --- |
| `containerPort` | Pod spec | the port the process listens on (documentation, and a name to refer to) |
| `port` | Service | the port clients use on the Service IP |
| `targetPort` | Service | where the Service forwards: a container port number or **name** |

Prefer a **named** `targetPort`: if the app moves from 8080 to 9090, you change the
container port once and the Service keeps working. A Service with a selector that matches
no Pod, or a `targetPort` the container does not listen on, is the most common reason for
"connection refused" in Kubernetes. The katas check both.

## EndpointSlices and kube-proxy

The **EndpointSlice controller** watches the Service and the Pods and writes
**EndpointSlices**: the IP and port of every matching Pod and whether it is **ready**. Only
ready Pods receive traffic, which is how readiness probes take a Pod out of rotation.

On every node, **kube-proxy** watches Services and EndpointSlices and programs the kernel
(iptables, IPVS or nftables rules) so that a connection to the virtual IP is rewritten to
one of the Pod IPs. No proxy process sits in the data path, and the virtual IP answers no
ping: it exists only as rules. Some CNI plugins (Cilium) replace kube-proxy with eBPF.

```powershell
kubectl get svc orders
kubectl get endpointslices -l kubernetes.io/service-name=orders
kubectl describe svc orders          # Endpoints: empty means the selector or readiness is wrong
```

## Service types

| Type | Reachable from | Use |
| --- | --- | --- |
| `ClusterIP` | inside the cluster | service-to-service traffic, the default |
| `NodePort` | `<any node IP>:30000-32767` | simple external access, or behind your own load balancer |
| `LoadBalancer` | an external IP from the cloud | expose one service directly; the cloud controller creates the load balancer |
| `ExternalName` | a DNS alias (CNAME) to a name outside | point `db` at `db.prod.example.com` |

Each type builds on the previous: a LoadBalancer has a NodePort, which has a ClusterIP. For
HTTP, one Ingress or Gateway in front of many ClusterIP Services is cheaper than one cloud
load balancer per service (Phase 2).

A **headless** Service (`clusterIP: None`) has no virtual IP: DNS returns the Pod IPs
directly. StatefulSets use it to give every Pod a stable name such as
`postgres-0.postgres.db.svc.cluster.local` (Phase 2).

## DNS

**CoreDNS** gives every Service a name:

```text
orders                              same namespace
orders.shop                         from another namespace
orders.shop.svc.cluster.local       fully qualified
```

A Pod's `/etc/resolv.conf` lists search domains (`shop.svc.cluster.local`,
`svc.cluster.local`, `cluster.local`), so a short name is tried in the Pod's own namespace
first. Calling a Service in another namespace needs at least `name.namespace`. Named ports
also get SRV records (`_http._tcp.orders.shop.svc.cluster.local`).

Other details worth knowing: `sessionAffinity: ClientIP` sticks a client to one Pod;
`internalTrafficPolicy: Local` and `externalTrafficPolicy: Local` keep traffic on the node
it arrived at (the second one preserves the client source IP).

## Summary

- A Service selects Pods by label and gives them one stable name and virtual IP.
- `port` is what clients use, `targetPort` is the container port: prefer a named one.
- EndpointSlices list the ready Pods; kube-proxy turns the virtual IP into Pod IPs.
- ClusterIP inside, NodePort and LoadBalancer outside, ExternalName as an alias, headless
  for direct Pod addresses.
- DNS names are `service`, `service.namespace` and `service.namespace.svc.cluster.local`.

## Further reading

- Service: <https://kubernetes.io/docs/concepts/services-networking/service/>
- EndpointSlices: <https://kubernetes.io/docs/concepts/services-networking/endpoint-slices/>
- DNS for Services and Pods: <https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/>
- Virtual IPs and service proxies: <https://kubernetes.io/docs/reference/networking/virtual-ips/>
