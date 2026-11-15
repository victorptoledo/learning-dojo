load /opt/dojo/k8s

@test "the ConfigMap greeter-config holds GREETING_NAME and LOG_LEVEL" {
    run get manifests ConfigMap greeter-config '.data.GREETING_NAME'
    assert_output "dojo"
    run get manifests ConfigMap greeter-config '.data.LOG_LEVEL'
    assert_output "info"
}

@test "runs 3 replicas" {
    run get manifests Deployment greeter '.spec.replicas'
    assert_output "3"
}

@test "the Deployment selects its own Pods by app.kubernetes.io/name" {
    policy manifests selectors
    run get manifests Deployment greeter '.spec.template.metadata.labels["app.kubernetes.io/name"]'
    assert_output "greeter"
}

@test "runs ghcr.io/acme/greeter:1.2.0 with port 8080 named http" {
    run get manifests Deployment greeter '.spec.template.spec.containers[] | select(.name == "greeter") | .image'
    assert_output "ghcr.io/acme/greeter:1.2.0"
    run get manifests Deployment greeter '.spec.template.spec.containers[0].ports[] | select(.name == "http") | .containerPort'
    assert_output "8080"
}

@test "loads every key of greeter-config with envFrom" {
    run get manifests Deployment greeter '[.spec.template.spec.containers[0].envFrom[]?.configMapRef.name] | join(",")'
    assert_output "greeter-config"
}

@test "probes readiness on /ready and liveness on /health, by port name" {
    policy manifests probes
    run get manifests Deployment greeter '.spec.template.spec.containers[0] | "\(.readinessProbe.httpGet.path) \(.readinessProbe.httpGet.port)"'
    assert_output "/ready http"
    run get manifests Deployment greeter '.spec.template.spec.containers[0] | "\(.livenessProbe.httpGet.path) \(.livenessProbe.httpGet.port)"'
    assert_output "/health http"
}

@test "requests 100m CPU and 128Mi memory, and caps memory at 256Mi" {
    policy manifests resources
    run get manifests Deployment greeter '.spec.template.spec.containers[0].resources | "\(.requests.cpu) \(.requests.memory) \(.limits.memory)"'
    assert_output "100m 128Mi 256Mi"
}

@test "rolls out with maxSurge 1 and maxUnavailable 0" {
    run get manifests Deployment greeter '.spec.strategy.rollingUpdate | "\(.maxSurge) \(.maxUnavailable)"'
    assert_output "1 0"
}

@test "the Service is a ClusterIP on port 80" {
    run get manifests Service greeter '"\(.spec.type // "ClusterIP") \(.spec.ports[0].port)"'
    assert_output "ClusterIP 80"
}

@test "the Service targets the named port http" {
    run get manifests Service greeter '.spec.ports[0].targetPort'
    assert_output "http"
}
