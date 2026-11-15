load /opt/dojo/k8s

pod='.spec.template.spec'

@test "the namespace enforces and warns on restricted" {
    run get manifests Namespace inventory '.metadata.labels["pod-security.kubernetes.io/enforce"]'
    assert_output "restricted"
    run get manifests Namespace inventory '.metadata.labels["pod-security.kubernetes.io/warn"]'
    assert_output "restricted"
}

@test "RBAC has no wildcards, escalation verbs or admin roles" {
    policy manifests least_privilege
}

@test "the Role allows only get and watch on the ConfigMap inventory-flags" {
    run get manifests Role inventory '[.rules[] | "\(.apiGroups | join(",")):\(.resources | join(",")):\(.resourceNames // [] | join(",")):\(.verbs | sort | join(","))"] | join(" ")'
    assert_output ":configmaps:inventory-flags:get,watch"
}

@test "the RoleBinding gives the Role to the inventory ServiceAccount only" {
    run get manifests RoleBinding inventory '"\(.roleRef.kind)/\(.roleRef.name) \([.subjects[] | "\(.kind)/\(.namespace)/\(.name)"] | join(","))"'
    assert_output "Role/inventory ServiceAccount/inventory/inventory"
}

@test "the Pods run as the inventory ServiceAccount" {
    run get manifests Deployment inventory "$pod"'.serviceAccountName'
    assert_output "inventory"
}

@test "the Pods run as UID 10001" {
    run get manifests Deployment inventory "$pod"' | (.containers[0].securityContext.runAsUser // .securityContext.runAsUser)'
    assert_output "10001"
}

@test "the root file system is read-only with an emptyDir at /tmp" {
    policy manifests read_only_root
    run get manifests Deployment inventory "$pod"' as $p | $p.containers[0].volumeMounts[]? | select(.mountPath == "/tmp") | .name as $n | $p.volumes[] | select(.name == $n) | has("emptyDir")'
    assert_output "true"
}

@test "a default deny covers ingress and egress for every Pod" {
    run k8s manifests '[.[] | select(.kind == "NetworkPolicy" and (.spec.podSelector | length) == 0 and ((.spec.ingress // []) | length) == 0 and ((.spec.egress // []) | length) == 0) | .spec.policyTypes | sort | join(",")] | unique | join(" ")'
    assert_output "Egress,Ingress"
}

@test "only the infra gateway Pods may call inventory on 8080" {
    run k8s manifests '[.[] | select(.kind == "NetworkPolicy" and .spec.podSelector.matchLabels["app.kubernetes.io/name"] == "inventory") | .spec.ingress[]? | "\([.from[] | "\(.namespaceSelector.matchLabels["kubernetes.io/metadata.name"] // "*")/\(.podSelector.matchLabels["app.kubernetes.io/name"] // "*")"] | join(",")) \([.ports[] | "\(.protocol // "TCP")/\(.port)"] | join(","))"] | join(" ")'
    assert_output "infra/gateway TCP/8080"
}

@test "inventory may reach only postgres on 5432 and DNS on 53" {
    run k8s manifests '[.[] | select(.kind == "NetworkPolicy" and .spec.podSelector.matchLabels["app.kubernetes.io/name"] == "inventory") | .spec.egress[]? | "\([.to[] | "\(.namespaceSelector.matchLabels["kubernetes.io/metadata.name"] // "-")/\(.podSelector.matchLabels["app.kubernetes.io/name"] // .podSelector.matchLabels["k8s-app"] // "*")"] | join(",")) \([.ports[] | "\(.protocol // "TCP")/\(.port)"] | sort | join(","))"] | sort | join(" ")'
    assert_output "-/postgres TCP/5432 kube-system/kube-dns TCP/53,UDP/53"
}
