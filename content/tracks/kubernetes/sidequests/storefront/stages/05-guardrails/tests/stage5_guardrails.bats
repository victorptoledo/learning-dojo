load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

# Quantities as numbers: CPU in cores, memory and storage in bytes.
QTY='def qty: tostring
  | capture("^(?<n>[0-9.]+)(?<u>[A-Za-z]*)$") as $q | ($q.n | tonumber) as $n
  | {"": 1, "m": 0.001, "k": 1e3, "M": 1e6, "G": 1e9, "T": 1e12,
     "Ki": 1024, "Mi": 1048576, "Gi": 1073741824, "Ti": 1099511627776}[$q.u] as $f
  | if $f == null then error("unknown unit \($q.u)") else $n * $f end;'

# peak FIELD: the sum of FIELD (requests.cpu, requests.memory, limits.memory or pods) over
# every workload at its HPA maximum, plus a 25 % surge (rounded up) for Deployments.
peak() {
    k8s manifests "$QTY"'
      . as $all
      | [ .[] | select(.kind == "Deployment" or .kind == "StatefulSet") | . as $w
          | ([$all[] | select(.kind == "HorizontalPodAutoscaler" and .spec.scaleTargetRef.name == $w.metadata.name) | .spec.maxReplicas] | first) as $max
          | ($max // $w.spec.replicas // 1) as $n
          | ($n + (if $w.kind == "Deployment" then ($n * 0.25 | ceil) else 0 end)) as $pods
          | if $field == "pods" then $pods
            else ($field | split(".")) as [$kind, $res]
              | $pods * ([$w | app_containers | .resources[$kind][$res] // "0" | qty] | add)
            end ]
      | add' --arg field "$1"
}

quota() {
    k8s manifests "$QTY"'[.[] | select(.kind == "ResourceQuota" and .metadata.name == "shop-quota") | .spec.hard[$key] // empty | qty] | first // "none"' --arg key "$1"
}

# can VERB GROUP RESOURCE: what the deployer ServiceAccount may do, through every
# RoleBinding and ClusterRoleBinding that names it.
can() {
    k8s manifests '. as $all
      | [ .[] | select(.kind == "RoleBinding" or .kind == "ClusterRoleBinding")
          | select(any(.subjects // [] | .[]; .kind == "ServiceAccount" and .name == "deployer" and .namespace == "shop"))
          | .roleRef as $ref
          | $all[] | select(.kind == $ref.kind and .metadata.name == $ref.name) | .rules // [] | .[]
          | select((.resourceNames // []) == [])
          | select((.apiGroups // []) | index($group) != null or index("*") != null)
          | select((.resources // []) | index($resource) != null or index("*") != null)
          | select((.verbs // []) | index($verb) != null or index("*") != null) ]
      | if length > 0 then "yes" else "no" end' --arg verb "$1" --arg group "$2" --arg resource "$3"
}

expect_can() {
    local got
    got=$(can "$2" "$3" "$4") || return 1
    [ "$got" = "$1" ] || { echo "can the deployer $2 $4 (group \"$3\")? $got, it should be $1"; return 1; }
}

@test "the namespace enforces the restricted standard" {
    run get manifests Namespace shop '.metadata.labels // {} | [.["pod-security.kubernetes.io/enforce"], .["pod-security.kubernetes.io/enforce-version"], .["pod-security.kubernetes.io/audit"], .["pod-security.kubernetes.io/warn"]] | map(. // "none") | join(" ")'
    assert_output "restricted v1.36 restricted restricted"
}

@test "the quota caps every kind of resource and forbids node ports and load balancers" {
    for key in requests.cpu requests.memory limits.memory pods persistentvolumeclaims requests.storage; do
        run quota "$key"
        refute_output "none"
    done
    run quota services.loadbalancers
    assert_output "0"
    run quota services.nodeports
    assert_output "0"
}

@test "the shop fits in the quota at its peak, during a rollout" {
    for key in requests.cpu requests.memory limits.memory pods; do
        need=$(peak "$key")
        have=$(quota "$key")
        run jq -n --argjson need "$need" --argjson have "$have" '$need <= $have'
        assert_output "true" || fail "$key: the peak needs $need, the quota allows $have"
    done
}

@test "the database claim fits in the storage quota" {
    run k8s manifests "$QTY"'[.[] | select(.kind == "StatefulSet") | (.spec.replicas // 1) as $n | .spec.volumeClaimTemplates // [] | .[] | {n: $n, size: (.spec.resources.requests.storage | qty)}]
      | "\(map(.n) | add) \(map(.n * .size) | add)"'
    read -r claims storage <<< "$output"
    run jq -n --argjson c "$claims" --argjson s "$storage" --argjson qc "$(quota persistentvolumeclaims)" --argjson qs "$(quota requests.storage)" '$c <= $qc and $s <= $qs'
    assert_output "true"
}

@test "the quota is not more than twice the peak" {
    for key in requests.cpu requests.memory; do
        need=$(peak "$key")
        have=$(quota "$key")
        run jq -n --argjson need "$need" --argjson have "$have" '$have <= 2 * $need'
        assert_output "true" || fail "$key: the quota allows $have, more than twice the peak of $need"
    done
}

@test "containers that forget their resources get defaults" {
    run get manifests LimitRange shop-defaults '.spec.limits[] | select(.type == "Container") | "\(.defaultRequest.cpu) \(.defaultRequest.memory) \(.default.memory) \(.max.memory)"'
    assert_output "100m 128Mi 256Mi 4Gi"
}

@test "the deployer is a namespaced role bound to its ServiceAccount" {
    run get manifests ServiceAccount deployer '.automountServiceAccountToken'
    assert_output "false"
    run get manifests RoleBinding shop-deployer '"\(.roleRef.kind)/\(.roleRef.name) " + (.subjects | map("\(.kind):\(.namespace)/\(.name)") | join(","))'
    assert_output "Role/shop-deployer ServiceAccount:shop/deployer"
    run count manifests ClusterRoleBinding
    assert_output "0"
    policy manifests least_privilege
}

@test "the deployer can roll out every part of the shop" {
    for verb in get list watch create update patch; do
        expect_can yes "$verb" apps deployments
        expect_can yes "$verb" apps statefulsets
        expect_can yes "$verb" "" services
        expect_can yes "$verb" "" configmaps
        expect_can yes "$verb" autoscaling horizontalpodautoscalers
        expect_can yes "$verb" policy poddisruptionbudgets
        expect_can yes "$verb" networking.k8s.io networkpolicies
    done
}

@test "the deployer can watch a rollout and read logs" {
    for verb in get list watch; do
        expect_can yes "$verb" apps replicasets
        expect_can yes "$verb" "" pods
        expect_can yes "$verb" "" pods/log
    done
    expect_can no create "" pods
    expect_can no delete "" pods
}

@test "the deployer cannot delete, read secrets, exec or escalate" {
    expect_can no delete apps deployments
    expect_can no delete "" services
    expect_can no get "" secrets
    expect_can no list "" secrets
    expect_can no create "" pods/exec
    expect_can no get "" pods/exec
    expect_can no create rbac.authorization.k8s.io roles
    expect_can no create rbac.authorization.k8s.io rolebindings
    expect_can no patch "" resourcequotas
    expect_can no patch "" limitranges
    expect_can no patch "" namespaces
    expect_can no patch "" serviceaccounts
}
