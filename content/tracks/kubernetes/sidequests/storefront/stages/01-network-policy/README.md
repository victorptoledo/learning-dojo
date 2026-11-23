Today every Pod in the cluster can reach every other Pod: anything that breaks into `web`
can talk to the database directly, and a compromised `api` can reach the cloud metadata
service and steal the node's credentials. Write the NetworkPolicies of `shop` (for example in
`netpol.yaml`) so that only these flows exist:

| From | To | Port |
| --- | --- | --- |
| the ingress controller (Pods in the namespace `ingress-nginx`) | `web` | TCP 8080 |
| `web` | `api` | TCP 8080 |
| `api` | `db` | TCP 5432 |
| `api` | the payment provider: any **public** address | TCP 443 |
| every Pod of `shop` | CoreDNS (Pods labelled `k8s-app: kube-dns` in `kube-system`) | UDP 53 and TCP 53 |

Everything else is denied, in both directions, including:

- the ingress controller reaching `api` or `db`; `web` reaching `db`; `db` or `api` calling
  back to `web`; any port other than the one in the table;
- a Pod in another namespace that copies `web`'s labels (a `podSelector` alone only matches
  Pods in the policy's own namespace, which is what you want here);
- `web` and `db` reaching the internet at all, and `api` reaching it on any port but 443;
- `api` reaching private addresses through its internet rule: the rule for the payment
  provider is an `ipBlock` for `0.0.0.0/0` **except** `10.0.0.0/8`, `172.16.0.0/12`,
  `192.168.0.0/16` and `169.254.0.0/16` (the link-local range of the cloud metadata service,
  `169.254.169.254`).

Start with a **default deny**: one policy whose empty `podSelector` selects every Pod of the
namespace, with `policyTypes: [Ingress, Egress]` and no rules. Then add one policy per flow
(or per part), each allowing only what the table says. Remember that a connection needs
**both** sides: the egress of the client and the ingress of the server.

Namespaces are matched by their automatic label `kubernetes.io/metadata.name`.

The dojo simulates the traffic from the policies, the Pod labels and the namespace labels in
your manifests, the way the NetworkPolicy API defines it.

> [!TIP]
> Forgetting DNS is the classic mistake: after a default deny, `api` cannot even resolve
> `db`. And NetworkPolicies do nothing on a cluster whose CNI does not enforce them
> (kindnet, older Flannel): on kind, install Calico or Cilium to try yours.
