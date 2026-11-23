# Shared by the tests of every stage: `source "$BATS_TEST_DIRNAME/helpers.sh"` after
# `load /opt/dojo/k8s`. The shop's manifests are the target `manifests` (every .yaml file
# at the top of the folder).

# A NetworkPolicy simulator, in jq, over the rendered objects. Traffic from SRC to DST on a
# port is allowed when SRC's egress allows it and DST's ingress allows it. A side is open
# when no policy selects that Pod for that direction; otherwise one rule of one selecting
# policy must match the peer and the port. Endpoints outside the namespace (the ingress
# controller, DNS, a stranger's Pod, internet addresses) have no policies of their own here.
NETPOL_DEFS='
def ip2n: split(".") | map(tonumber) | .[0] * 16777216 + .[1] * 65536 + .[2] * 256 + .[3];
def in_cidr($ip): split("/") as [$net, $len]
  | pow(2; 32 - ($len | tonumber)) as $size
  | (($ip | ip2n) / $size | floor) == (($net | ip2n) / $size | floor);
def sel_match($labels):
  ((.matchLabels // {}) | to_entries | all(.value == ($labels[.key] // null)))
  and ((.matchExpressions // []) | all(
        .key as $k | ($labels[$k] // null) as $v
        | if .operator == "In" then $v != null and ((.values // []) | index($v)) != null
          elif .operator == "NotIn" then $v == null or ((.values // []) | index($v)) == null
          elif .operator == "Exists" then $labels | has($k)
          elif .operator == "DoesNotExist" then ($labels | has($k)) | not
          else false end));
def peer_match($peer; $ep; $ns):
  if $peer.ipBlock != null then
    $ep.ip != null
    and ($peer.ipBlock.cidr | in_cidr($ep.ip))
    and ([($peer.ipBlock.except // [])[] | in_cidr($ep.ip)] | any | not)
  else
    $ep.labels != null
    and (if $peer.namespaceSelector != null then ($peer.namespaceSelector | sel_match($ep.nsLabels))
         else $ep.ns == $ns end)
    and (if $peer.podSelector != null then ($peer.podSelector | sel_match($ep.labels)) else true end)
  end;
def port_match($ports; $port; $dst):
  ($ports // []) as $ps
  | ($ps | length) == 0 or ([$ps[] | select((.protocol // "TCP") == $port.protocol)
      | if .port == null then true
        elif (.port | type) == "number" then
          if .endPort != null then $port.number >= .port and $port.number <= .endPort
          else .port == $port.number end
        else .port as $name
          | [($dst.ports // [])[] | select(.name == $name and .containerPort == $port.number and .protocol == $port.protocol)] | length > 0
        end] | any);
def policy_types: .spec.policyTypes // (["Ingress"] + (if .spec.egress != null then ["Egress"] else [] end));
def selects($ep): $ep.labels != null and .metadata.namespace == $ep.ns and ((.spec.podSelector // {}) | sel_match($ep.labels));
def side_open($pols; $ep; $type; $field; $peers; $peer; $port; $dst):
  [$pols[] | select(selects($ep)) | select(policy_types | index($type) != null)] as $sel
  | ($sel | length) == 0
    or ([$sel[] | .metadata.namespace as $ns | (.spec[$field] // [])[]
         | ((.[$peers] // []) as $list | ($list | length) == 0 or ([$list[] | peer_match(.; $peer; $ns)] | any))
           and port_match(.ports; $port; $dst)] | any);
def allowed($src; $dst; $port):
  [.[] | select(.kind == "NetworkPolicy")] as $pols
  | side_open($pols; $src; "Egress"; "egress"; "to"; $dst; $port; $dst)
    and side_open($pols; $dst; "Ingress"; "ingress"; "from"; $src; $port; $dst);
def ns_labels($ns): ([.[] | select(.kind == "Namespace" and .metadata.name == $ns) | .metadata.labels // {}] | first // {})
  + {"kubernetes.io/metadata.name": $ns};
def endpoint($name):
  . as $all
  | {web: "10.244.1.10", api: "10.244.1.20", db: "10.244.1.30"} as $ips
  | if $ips[$name] != null then
      ([$all[] | select((.kind == "Deployment" or .kind == "StatefulSet") and .metadata.name == $name)] | first) as $w
      | ($w.metadata.namespace // "default") as $ns
      | {ns: $ns, nsLabels: ($all | ns_labels($ns)), labels: ($w | pod_labels), ip: $ips[$name],
         ports: [$w | app_containers | (.ports // [])[] | {name, containerPort, protocol: (.protocol // "TCP")}]}
    elif $name == "ingress" then
      {ns: "ingress-nginx", nsLabels: {"kubernetes.io/metadata.name": "ingress-nginx"},
       labels: {"app.kubernetes.io/name": "ingress-nginx"}, ip: "10.244.2.5", ports: []}
    elif $name == "dns" then
      {ns: "kube-system", nsLabels: {"kubernetes.io/metadata.name": "kube-system"},
       labels: {"k8s-app": "kube-dns"}, ip: "10.244.3.3",
       ports: [{name: "dns", containerPort: 53, protocol: "UDP"}, {name: "dns-tcp", containerPort: 53, protocol: "TCP"}]}
    elif $name == "stranger" then
      {ns: "other", nsLabels: {"kubernetes.io/metadata.name": "other"},
       labels: ([$all[] | select(.kind == "Deployment" and .metadata.name == "web") | pod_labels] | first // {}),
       ip: "10.244.4.4", ports: []}
    elif $name == "internet" then {ip: "203.0.113.10"}
    elif $name == "metadata" then {ip: "169.254.169.254"}
    elif $name == "node" then {ip: "192.168.10.5"}
    else error("unknown endpoint " + $name) end;
'

# reach SRC DST PROTOCOL PORT: "allowed" or "denied". Endpoints: web, api, db (the shop's
# Pods), ingress (the ingress-nginx controller), dns (CoreDNS in kube-system), stranger (a
# Pod with web's labels in another namespace), internet (203.0.113.10), metadata
# (169.254.169.254, the cloud metadata service) and node (192.168.10.5).
reach() {
    k8s manifests "$NETPOL_DEFS"'
      endpoint($s) as $src | endpoint($d) as $dst
      | if allowed($src; $dst; {protocol: $proto, number: ($port | tonumber)}) then "allowed" else "denied" end' \
        --arg s "$1" --arg d "$2" --arg proto "$3" --arg port "$4"
}

# expect_reach SRC DST PROTOCOL PORT allowed|denied: fails with the flow in the message.
expect_reach() {
    local got
    got=$(reach "$1" "$2" "$3" "$4") || return 1
    if [ "$got" != "$5" ]; then
        echo "$1 -> $2 on $3/$4 is $got, it should be $5"
        return 1
    fi
}
