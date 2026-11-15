# Ingress and the Gateway API

A LoadBalancer Service per application gets expensive and cannot route by host name or path.
For HTTP, clusters run one **L7 proxy** (NGINX, Envoy, HAProxy, Traefik, a cloud load
balancer) and describe the routing to it with Kubernetes objects: the older **Ingress**, or
its successor, the **Gateway API**.

## Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: shop
spec:
  ingressClassName: nginx                 # which controller handles it
  tls:
    - hosts: [shop.example.com]
      secretName: shop-tls                # a kubernetes.io/tls Secret
  rules:
    - host: shop.example.com
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service: { name: api, port: { name: http } }
          - path: /
            pathType: Prefix
            backend:
              service: { name: web, port: { number: 80 } }
```

An Ingress does nothing on its own. An **Ingress controller** (a Deployment running a proxy,
behind one LoadBalancer Service) watches Ingress objects and configures itself. The
**IngressClass** names the controller, so a cluster can run several; one class can be the
default.

`pathType` matters: `Prefix` matches by path segments (`/api` matches `/api/orders`, not
`/apiv2`), `Exact` matches one path, and `ImplementationSpecific` leaves it to the
controller. The most specific match wins.

TLS certificates come from Secrets, usually issued and renewed by **cert-manager** from
Let's Encrypt or an internal CA, triggered by an annotation on the Ingress.

The weakness of Ingress is its small common core. Rewrites, timeouts, header matching,
canaries and rate limits are all controller-specific **annotations**
(`nginx.ingress.kubernetes.io/...`), so manifests are not portable and one object mixes the
platform team's concerns with the application team's.

## The Gateway API

The **Gateway API** (`gateway.networking.k8s.io`, generally available since 2023) replaces
that with expressive, typed objects split by role:

| Object | Owned by | Says |
| --- | --- | --- |
| **GatewayClass** | the infrastructure provider | which controller implements Gateways of this class |
| **Gateway** | the platform or cluster operator | listeners: ports, protocols, host names, TLS, which namespaces may attach routes |
| **HTTPRoute** (also GRPCRoute, TLSRoute…) | application teams | matches (path, header, method, query) and backends, with weights and filters |

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata: { name: public, namespace: infra }
spec:
  gatewayClassName: envoy
  listeners:
    - name: https
      protocol: HTTPS
      port: 443
      hostname: "*.example.com"
      tls:
        certificateRefs: [{ name: wildcard-tls }]
      allowedRoutes:
        namespaces: { from: Selector, selector: { matchLabels: { expose: "true" } } }
---
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata: { name: api, namespace: shop }
spec:
  parentRefs: [{ name: public, namespace: infra }]
  hostnames: [shop.example.com]
  rules:
    - matches: [{ path: { type: PathPrefix, value: /api } }]
      backendRefs:
        - { name: api, port: 80, weight: 90 }
        - { name: api-v2, port: 80, weight: 10 }    # a canary, in the spec itself
```

The Gateway decides which namespaces may attach; a route in another namespace referring to
a Service across namespaces needs a **ReferenceGrant** from the target namespace. Filters
cover redirects, rewrites, header changes and mirroring without annotations. Many
controllers implement it (Envoy Gateway, Istio, Cilium, NGINX Gateway Fabric, cloud load
balancers), and **status** on every object reports whether it was accepted and why not.

The Ingress API is frozen: it stays supported, but new features land only in the Gateway
API. The community ingress-nginx controller is being retired, so new clusters should start
with the Gateway API.

## Choosing

- An existing cluster with Ingress and simple host/path routing: keep it, plan a migration.
- New work, several teams, canaries, header routing or gRPC: the Gateway API.
- Either way, the backends are ordinary ClusterIP Services, and TLS keys live in Secrets
  that only the proxy needs to read.

## Summary

- Ingress and Gateway objects describe routing; a controller (a proxy) implements it.
- An Ingress holds host and path rules, TLS Secrets and an `ingressClassName`; extra
  features live in controller-specific annotations.
- The Gateway API splits roles: GatewayClass (provider), Gateway (platform), HTTPRoute (app).
- Routes attach with `parentRefs`; weights, filters and matches are part of the spec.
- New clusters should prefer the Gateway API; cert-manager automates certificates for both.

## Further reading

- Ingress: <https://kubernetes.io/docs/concepts/services-networking/ingress/>
- Ingress controllers: <https://kubernetes.io/docs/concepts/services-networking/ingress-controllers/>
- Gateway API in Kubernetes: <https://kubernetes.io/docs/concepts/services-networking/gateway/>
- Gateway API documentation: <https://gateway-api.sigs.k8s.io/>
- cert-manager: <https://cert-manager.io/docs/>
