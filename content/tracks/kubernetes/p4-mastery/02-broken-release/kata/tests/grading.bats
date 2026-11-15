load /opt/dojo/k8s

container='.spec.template.spec.containers[] | select(.name == "checkout")'

@test "runs the released image ghcr.io/acme/checkout:2.4.0" {
    run get manifests Deployment checkout "$container"' | .image'
    assert_output "ghcr.io/acme/checkout:2.4.0"
}

@test "the probes aim at declared ports" {
    policy manifests probe_ports
}

@test "the readiness probe checks /ready on the http port" {
    run get manifests Deployment checkout "$container"' | .readinessProbe.httpGet | "\(.path) \(.port)"'
    assert_output --regexp '^/ready (http|8080)$'
}

@test "the liveness probe checks only the process, on /health" {
    run get manifests Deployment checkout "$container"' | .livenessProbe.httpGet.path'
    assert_output "/health"
}

@test "the Service selects the checkout Pods and reaches the http port" {
    policy manifests service_targets
    run get manifests Service checkout '.spec.ports[0] | "\(.port) \(.targetPort)"'
    assert_output --regexp '^80 (http|8080)$'
}

@test "the Service does not select on a version label" {
    run get manifests Service checkout '.spec.selector | keys | join(",")'
    assert_output "app.kubernetes.io/name"
}

@test "memory is sized from the capacity review" {
    run get manifests Deployment checkout "$container"' | .resources | "\(.requests.memory) \(.limits.memory)"'
    assert_output "512Mi 768Mi"
}

@test "a drain can evict one Pod at a time" {
    run get manifests PodDisruptionBudget checkout '"\(.spec.maxUnavailable // "-") \(.spec.minAvailable // "-")"'
    assert_output --regexp '^(1 -|- 2)$'
}

@test "what worked still works: 3 replicas, selectors, probes, resources, the ConfigMap" {
    run get manifests Deployment checkout '.spec.replicas'
    assert_output "3"
    policy manifests selectors
    policy manifests probes
    policy manifests resources
    run get manifests ConfigMap checkout-config '.data | keys | join(",")'
    assert_output "LOG_LEVEL,PAYMENTS_URL"
}

@test "PAYMENTS_URL comes from the ConfigMap key PAYMENTS_URL" {
    run get manifests Deployment checkout "$container"' | .env[] | select(.name == "PAYMENTS_URL") | .valueFrom.configMapKeyRef | "\(.name) \(.key)"'
    assert_output "checkout-config PAYMENTS_URL"
}
