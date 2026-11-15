load /opt/dojo/k8s

@test "staging still renders 1 replica in catalog-staging" {
    run get staging Deployment catalog '"\(.metadata.namespace) \(.spec.replicas)"'
    assert_output "catalog-staging 1"
}

@test "prod runs 4 replicas" {
    run get prod Deployment catalog '.spec.replicas'
    assert_output "4"
}

@test "prod runs ghcr.io/acme/catalog:2.3.1" {
    run get prod Deployment catalog '.spec.template.spec.containers[] | select(.name == "catalog") | .image'
    assert_output "ghcr.io/acme/catalog:2.3.1"
}

@test "prod keeps the selectors, the Service wiring and the probes" {
    policy prod selectors
    policy prod service_targets
    policy prod probes
}

@test "prod requests 500m CPU and 512Mi memory with a 512Mi limit" {
    run get prod Deployment catalog '.spec.template.spec.containers[] | select(.name == "catalog") | .resources | "\(.requests.cpu) \(.requests.memory) \(.limits.memory)"'
    assert_output "500m 512Mi 512Mi"
}

@test "prod generates catalog-config with a hash suffix and LOG_LEVEL=warn" {
    run names prod ConfigMap
    assert_output --regexp '^catalog-config-[a-z0-9]+$'
    name=$output
    run get prod ConfigMap "$name" '"\(.data.LOG_LEVEL) \(.data.PAGE_SIZE)"'
    assert_output "warn 50"
}

@test "the prod Deployment reads the generated ConfigMap" {
    name=$(names prod ConfigMap)
    run get prod Deployment catalog '.spec.template.spec.containers[0].envFrom[0].configMapRef.name'
    assert_output "$name"
}

@test "prod spreads the replicas over zones" {
    run get prod Deployment catalog '.spec.template.spec.topologySpreadConstraints[]? | select(.topologyKey == "topology.kubernetes.io/zone") | "\(.maxSkew) \(.whenUnsatisfiable)"'
    assert_output "1 DoNotSchedule"
    run get prod Deployment catalog '. as $d | .spec.template.spec.topologySpreadConstraints[0].labelSelector.matchLabels as $s | matches($s; $d | pod_labels)'
    assert_output "true"
}

@test "a PodDisruptionBudget allows one catalog Pod down at a time" {
    run get prod PodDisruptionBudget catalog '.spec.maxUnavailable'
    assert_output "1"
    pod_labels=$(get prod Deployment catalog '.spec.template.metadata.labels | tojson')
    run get prod PodDisruptionBudget catalog '(.spec.selector.matchLabels // {}) as $s | ($s | length) > 0 and matches($s; $labels)' --argjson labels "$pod_labels"
    assert_output "true"
}

@test "an HTTPRoute sends catalog.example.com to the catalog Service" {
    run get prod HTTPRoute catalog '.spec.parentRefs[0] | "\(.name) \(.namespace)"'
    assert_output "public infra"
    run get prod HTTPRoute catalog '.spec.hostnames | join(",")'
    assert_output "catalog.example.com"
    run get prod HTTPRoute catalog '.spec.rules[0] | "\(.matches[0].path.type) \(.matches[0].path.value) \(.backendRefs[0].name) \(.backendRefs[0].port)"'
    assert_output "PathPrefix / catalog 80"
}
