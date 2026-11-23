load /opt/dojo/k8s

# probe DEPLOYMENT FIELD: "path port" of one probe of the first container, or "none".
probe() {
    get manifests Deployment "$1" '.spec.template.spec.containers[0][$field] // null
      | if . == null then "none" else "\(.httpGet.path // "-") \(.httpGet.port // "-")" end' --arg field "$2"
}

# The QoS class Kubernetes would give the Pods of a Deployment.
qos() {
    get manifests Deployment "$1" '
      [.spec.template.spec.containers[] | .resources // {}] as $r
      | if all($r[]; (.requests // {}) == {} and (.limits // {}) == {}) then "BestEffort"
        elif all($r[]; .limits.cpu != null and .limits.memory != null
                 and (.requests.cpu // .limits.cpu) == .limits.cpu
                 and (.requests.memory // .limits.memory) == .limits.memory) then "Guaranteed"
        else "Burstable" end'
}

# Seconds a startup probe allows: periodSeconds (default 10) times failureThreshold (default 3).
startup_budget() {
    get manifests Deployment "$1" '.spec.template.spec.containers[0].startupProbe // null
      | if . == null then 0 else (.initialDelaySeconds // 0) + (.periodSeconds // 10) * (.failureThreshold // 3) end'
}

@test "every container has probes and resources, on declared ports" {
    policy manifests probes
    policy manifests resources
    policy manifests probe_ports
}

@test "the API gets a minute to start before its probes count" {
    run probe board-api startupProbe
    assert_output "/health http"
    run startup_budget board-api
    [ "$output" -ge 60 ] || fail "the startup probe gives up after ${output}s, not 60"
}

@test "the API is ready on /ready and alive on /health" {
    run probe board-api readinessProbe
    assert_output "/ready http"
    run probe board-api livenessProbe
    assert_output "/health http"
    run get manifests Deployment board-api '.spec.template.spec.containers[0].livenessProbe.initialDelaySeconds // 0'
    assert_output "0"
}

@test "the web part is ready on / and alive on /healthz with no startup probe" {
    run probe board-web readinessProbe
    assert_output "/ http"
    run probe board-web livenessProbe
    assert_output "/healthz http"
    run probe board-web startupProbe
    assert_output "none"
}

@test "the API is Guaranteed with 250m CPU and 256Mi memory" {
    run qos board-api
    assert_output "Guaranteed"
    run get manifests Deployment board-api '.spec.template.spec.containers[0].resources | "\(.requests.cpu) \(.requests.memory) \(.limits.cpu) \(.limits.memory)"'
    assert_output "250m 256Mi 250m 256Mi"
}

@test "the web part is Burstable with no CPU limit" {
    run qos board-web
    assert_output "Burstable"
    run get manifests Deployment board-web '.spec.template.spec.containers[0].resources | "\(.requests.cpu) \(.requests.memory) \(.limits.memory) \(.limits.cpu // "none")"'
    assert_output "50m 64Mi 128Mi none"
}
