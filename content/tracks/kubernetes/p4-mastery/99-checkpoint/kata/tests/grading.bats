load /opt/dojo/k8s

KATA="$BATS_TEST_DIRNAME/.."
CRD="$KATA/chart/crds/databases.yaml"
FIXTURES="$BATS_TEST_DIRNAME/fixtures"

# crd FILTER: a jq filter over the CRD file, raw output.
crd() {
    yq -o=json '.' "$CRD" | jq -r "$@"
}

# verdict FIXTURE [YQ_EXPRESSION]: "accepted", or "rejected: <why>", for the fixture (changed
# by the yq expression first) against the CRD's schema for its apiVersion.
verdict() {
    local fixture="$FIXTURES/$1.yaml" change=${2:-.}
    jq -n -r -L "$BATS_TEST_DIRNAME" \
        --argjson crd "$(yq -o=json -I=0 '.' "$CRD")" \
        --argjson cr "$(yq -o=json -I=0 "$change" "$fixture")" \
        'include "schema"; $cr | validate_cr($crd) | if length == 0 then "accepted" else "rejected: " + join("; ") end'
}

# rejected FIXTURE YQ_EXPRESSION PATH: the changed fixture is rejected, because of PATH.
rejected() {
    run verdict "$1" "$2"
    assert_output --regexp "^rejected: "
    assert_output --partial "$3:"
}

# grants TARGET KIND NAME: the rules of one role as "apiGroup/resource:verb,verb" lines.
grants() {
    get "$1" "$2" "$3" '[.rules[] | (.apiGroups // [""])[] as $g | .resources[] as $r
        | "\($g)/\($r):\(.verbs | sort | join(","))"] | sort | .[]'
}

# sorted LINES...: the lines in jq's (code point) order.
sorted() {
    printf '%s\n' "$@" | LC_ALL=C sort
}

# render ARGS...: helm template of the chart as the release db-operator in platform.
render() {
    helm template db-operator "$KATA/chart" --namespace platform "$@"
}

# ---- the CRD -----------------------------------------------------------------------------

@test "the CRD keeps its names and marks v1alpha1 as deprecated with a warning about v1" {
    run crd '.spec | "\(.group) \(.scope) \(.names.kind) \(.names.plural) \(.names.shortNames | join(","))"'
    assert_output "platform.acme.io Namespaced Database databases db"
    run crd '.spec.versions[] | select(.name == "v1alpha1") | "\(.served) \(.storage) \(.deprecated)"'
    assert_output "true false true"
    run crd '.spec.versions[] | select(.name == "v1alpha1") | .deprecationWarning // ""'
    assert_output --regexp 'platform\.acme\.io/v1([^a-z0-9]|$)'
}

@test "every version has the status subresource" {
    run crd '[.spec.versions[] | "\(.name) \(.subresources.status != null)"] | join(",")'
    assert_output "v1 true,v1alpha1 true"
}

@test "kubectl get databases shows the version, size, readiness and age" {
    run crd '.spec.versions[] | select(.name == "v1") | [.additionalPrinterColumns[]? | "\(.name)=\(.jsonPath)"] | join(" ")'
    assert_output 'Version=.spec.engineVersion Size=.spec.size Ready=.status.conditions[?(@.type=="Ready")].status Age=.metadata.creationTimestamp'
    run crd '.spec.versions[] | select(.name == "v1") | .additionalPrinterColumns[] | select(.name == "Age") | .type'
    assert_output "date"
}

@test "valid Databases are accepted in v1 and in v1alpha1" {
    run verdict v1
    assert_output "accepted"
    run verdict v1-backup
    assert_output "accepted"
    run verdict v1alpha1
    assert_output "accepted"
}

@test "engineVersion, size and storageGB are required" {
    for field in engineVersion size storageGB; do
        rejected v1 "del(.spec.$field)" ".spec.$field"
    done
}

@test "engineVersion is the string 17 or 18 and size is small, medium or large" {
    rejected v1 '.spec.engineVersion = "16"' ".spec.engineVersion"
    rejected v1 '.spec.engineVersion = 18' ".spec.engineVersion"
    rejected v1 '.spec.size = "xl"' ".spec.size"
    rejected v1alpha1 '.spec.size = "xl"' ".spec.size"
}

@test "storageGB is a whole number of gigabytes from 10 to 1000" {
    for good in 10 1000; do
        run verdict v1 ".spec.storageGB = $good"
        assert_output "accepted"
    done
    for bad in 9 1001 20.5 '"100"'; do
        rejected v1 ".spec.storageGB = $bad" ".spec.storageGB"
    done
}

@test "unknown fields in spec are rejected, so typos do not vanish silently" {
    rejected v1 '.spec.storageGb = 20' ".spec.storageGb"
    rejected v1 '.spec.replicas = 3' ".spec.replicas"
    rejected v1alpha1 '.spec.extra = true' ".spec.extra"
}

@test "backup takes a schedule string and 1 to 90 days of retention, and nothing else" {
    rejected v1-backup '.spec.backup.retentionDays = 0' ".spec.backup.retentionDays"
    rejected v1-backup '.spec.backup.retentionDays = 91' ".spec.backup.retentionDays"
    rejected v1-backup '.spec.backup.schedule = 3' ".spec.backup.schedule"
    rejected v1-backup '.spec.backup.keepForever = true' ".spec.backup.keepForever"
}

@test "status is the controller's: any shape is kept" {
    run verdict v1-backup '.status.observedGeneration = 4 | .status.endpoint = "billing-rw.finance:5432"'
    assert_output "accepted"
}

@test "only served versions are accepted" {
    run verdict v1
    assert_output "accepted"
    run verdict v1 '.apiVersion = "platform.acme.io/v1beta1"'
    assert_output "rejected: the version v1beta1 is not served"
}

@test "a CEL transition rule stops storageGB from shrinking, in every version" {
    run crd '[.spec.versions[] | .schema.openAPIV3Schema.properties.spec.properties.storageGB as $s
        | "\(.name) \([($s["x-kubernetes-validations"] // [])[] | select(.message != null and .message != "") | .rule | gsub("\\s"; "")] | map(select(. == "self>=oldSelf" or . == "oldSelf<=self")) | length > 0)"] | join(",")'
    assert_output "v1 true,v1alpha1 true"
}

# ---- RBAC ----------------------------------------------------------------------------------

@test "the controller's ClusterRole grants exactly what it reconciles" {
    run grants prod ClusterRole db-operator
    assert_output "$(sorted \
        'platform.acme.io/databases:get,list,patch,update,watch' \
        'platform.acme.io/databases/status:get,patch,update' \
        'platform.acme.io/databases/finalizers:update' \
        'apps/statefulsets:create,get,list,patch,update,watch' \
        '/services:create,get,list,patch,update,watch' \
        '/secrets:create,get,list,patch,update,watch' \
        'events.k8s.io/events:create,patch')"
}

@test "nothing is bound to an admin role and no rule uses wildcards or escalation verbs" {
    policy prod least_privilege
}

@test "the ClusterRole and a leader-election Role are bound to the controller's ServiceAccount" {
    run get prod ClusterRoleBinding db-operator '"\(.roleRef.kind)/\(.roleRef.name) " + ([.subjects[] | "\(.kind)/\(.namespace)/\(.name)"] | join(","))'
    assert_output "ClusterRole/db-operator ServiceAccount/platform/db-operator"
    run grants prod Role db-operator-leader-election
    assert_output "coordination.k8s.io/leases:create,get,list,patch,update,watch"
    run get prod RoleBinding db-operator-leader-election '"\(.roleRef.kind)/\(.roleRef.name) " + ([.subjects[] | "\(.kind)/\(.namespace)/\(.name)"] | join(","))'
    assert_output "Role/db-operator-leader-election ServiceAccount/platform/db-operator"
    run count prod ClusterRoleBinding
    assert_output "1"
}

@test "aggregated ClusterRoles let namespace editors manage Databases and viewers read them" {
    run get prod ClusterRole db-operator-edit '.metadata.labels | "\(.["rbac.authorization.k8s.io/aggregate-to-edit"]) \(.["rbac.authorization.k8s.io/aggregate-to-admin"])"'
    assert_output "true true"
    run grants prod ClusterRole db-operator-edit
    assert_output "platform.acme.io/databases:create,delete,deletecollection,get,list,patch,update,watch"
    run get prod ClusterRole db-operator-view '.metadata.labels["rbac.authorization.k8s.io/aggregate-to-view"]'
    assert_output "true"
    run grants prod ClusterRole db-operator-view
    assert_output "platform.acme.io/databases:get,list,watch"
}

# ---- the controller --------------------------------------------------------------------------

@test "the controller is pulled by the digest from the values, and a release without one fails" {
    run get prod Deployment db-operator '.spec.template.spec.containers[0].image'
    assert_output "ghcr.io/acme/db-operator@sha256:4f1c2a9e8b7d6c5f4e3d2c1b0a99887766554433221100ffeeddccbbaa998877"
    policy prod pinned_images
    run render
    assert_failure
    assert_output --partial "image.digest is required"
}

@test "two controllers run with leader election, and two without it do not render" {
    run get prod Deployment db-operator '"\(.spec.replicas) \(.spec.template.spec.containers[0].args | index("--leader-elect=true") != null)"'
    assert_output "2 true"
    run render -f "$KATA/chart/values-prod.yaml" --set leaderElection.enabled=false
    assert_failure
    assert_output --partial "leaderElection"
    run render -f "$KATA/chart/values-prod.yaml" --set leaderElection.enabled=false --set replicas=1
    assert_success
    refute_output --partial "--leader-elect"
}

@test "the controller meets the restricted profile with a read-only root" {
    policy prod restricted
    policy prod read_only_root
}

@test "readiness and liveness use the health port, and the metrics Service still works" {
    run get prod Deployment db-operator '.spec.template.spec.containers[0] | "\(.readinessProbe.httpGet.path) \(.readinessProbe.httpGet.port) \(.livenessProbe.httpGet.path) \(.livenessProbe.httpGet.port)"'
    assert_output --regexp '^/readyz (health|8081) /healthz (health|8081)$'
    policy prod probe_ports
    policy prod selectors
    policy prod service_targets
    policy prod resources
}

@test "the controllers spread over nodes and a drain keeps one running" {
    run get prod Deployment db-operator '. as $d | [.spec.template.spec.topologySpreadConstraints[]? | select(.topologyKey == "kubernetes.io/hostname" and .maxSkew == 1) | matches(.labelSelector.matchLabels; $d | pod_labels) and (.labelSelector.matchLabels | length) > 0] | any'
    assert_output "true"
    run get prod PodDisruptionBudget db-operator '.spec.minAvailable'
    assert_output "1"
    pod_labels=$(get prod Deployment db-operator '.spec.template.metadata.labels | tojson')
    run get prod PodDisruptionBudget db-operator '(.spec.selector.matchLabels // {}) as $s | ($s | length) > 0 and matches($s; $labels)' --argjson labels "$pod_labels"
    assert_output "true"
}
