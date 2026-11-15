load /opt/dojo/k8s

@test "the defaults render 1 replica and no HPA or PDB" {
    run get default Deployment ledger '.spec.replicas'
    assert_output "1"
    run count default HorizontalPodAutoscaler
    assert_output "0"
    run count default PodDisruptionBudget
    assert_output "0"
}

@test "both renderings wire selectors, the Service, probes and resources" {
    for target in default prod; do
        policy "$target" selectors
        policy "$target" service_targets
        policy "$target" probes
        policy "$target" resources
    done
}

@test "the rollout never loses capacity" {
    for target in default prod; do
        run get "$target" Deployment ledger '"\(.spec.strategy.type // "RollingUpdate") \(.spec.strategy.rollingUpdate.maxSurge) \(.spec.strategy.rollingUpdate.maxUnavailable)"'
        assert_output "RollingUpdate 25% 0"
    done
}

@test "a config checksum restarts the Pods when the config changes" {
    run get default Deployment ledger '.spec.template.metadata.annotations["checksum/config"] // ""'
    assert_output --regexp '^[0-9a-f]{64}$'
    default_sum=$output
    run get prod Deployment ledger '.spec.template.metadata.annotations["checksum/config"] // ""'
    assert_output --regexp '^[0-9a-f]{64}$'
    [ "$output" != "$default_sum" ] || fail "the checksum does not change when the config changes"
}

@test "prod leaves replicas to the HPA" {
    run get prod Deployment ledger '.spec.replicas // "unset"'
    assert_output "unset"
}

@test "prod runs ledger 1.8.0 with LOG_LEVEL warn" {
    run get prod Deployment ledger '.spec.template.spec.containers[0].image'
    assert_output "ghcr.io/acme/ledger:1.8.0"
    run get prod ConfigMap ledger '.data.LOG_LEVEL'
    assert_output "warn"
}

@test "prod requests 250m CPU and 256Mi memory with a 512Mi limit" {
    run get prod Deployment ledger '.spec.template.spec.containers[0].resources | "\(.requests.cpu) \(.requests.memory) \(.limits.memory)"'
    assert_output "250m 256Mi 512Mi"
}

@test "prod scales from 3 to 10 replicas at 70 % CPU" {
    run get prod HorizontalPodAutoscaler ledger '"\(.spec.minReplicas) \(.spec.maxReplicas)"'
    assert_output "3 10"
    run get prod HorizontalPodAutoscaler ledger '.spec.metrics[] | select(.type == "Resource" and .resource.name == "cpu") | .resource.target | "\(.type) \(.averageUtilization)"'
    assert_output "Utilization 70"
}

@test "prod scales down slowly" {
    run get prod HorizontalPodAutoscaler ledger '.spec.behavior.scaleDown.stabilizationWindowSeconds'
    assert_output "300"
}

@test "prod keeps 2 Pods available during drains" {
    run get prod PodDisruptionBudget ledger '.spec.minAvailable'
    assert_output "2"
    pod_labels=$(get prod Deployment ledger '.spec.template.metadata.labels | tojson')
    run get prod PodDisruptionBudget ledger '(.spec.selector.matchLabels // {}) as $s | ($s | length) > 0 and matches($s; $labels)' --argjson labels "$pod_labels"
    assert_output "true"
}
