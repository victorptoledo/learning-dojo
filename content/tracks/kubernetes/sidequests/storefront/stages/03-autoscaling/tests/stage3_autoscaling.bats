load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

metrics() {
    get manifests HorizontalPodAutoscaler "$1" '[.spec.metrics // [] | .[] | select(.type == "Resource") | "\(.resource.name)=\(.resource.target.type):\(.resource.target.averageUtilization)"] | sort | join(" ")'
}

@test "web scales from 3 to 12 at 70 % CPU" {
    run get manifests HorizontalPodAutoscaler web '"\(.spec.scaleTargetRef.apiVersion) \(.spec.scaleTargetRef.kind)/\(.spec.scaleTargetRef.name) \(.spec.minReplicas) \(.spec.maxReplicas)"'
    assert_output "apps/v1 Deployment/web 3 12"
    run metrics web
    assert_output "cpu=Utilization:70"
}

@test "api scales from 3 to 10 on CPU and memory" {
    run get manifests HorizontalPodAutoscaler api '"\(.spec.scaleTargetRef.apiVersion) \(.spec.scaleTargetRef.kind)/\(.spec.scaleTargetRef.name) \(.spec.minReplicas) \(.spec.maxReplicas)"'
    assert_output "apps/v1 Deployment/api 3 10"
    run metrics api
    assert_output "cpu=Utilization:65 memory=Utilization:80"
}

@test "both scale up fast" {
    for name in web api; do
        run get manifests HorizontalPodAutoscaler "$name" '.spec.behavior.scaleUp // {} | "\(.stabilizationWindowSeconds) \(.selectPolicy) " + ([.policies // [] | .[] | "\(.type):\(.value)/\(.periodSeconds)"] | sort | join(","))'
        assert_output "0 Max Percent:100/60,Pods:4/60"
    done
}

@test "both scale down slowly" {
    for name in web api; do
        run get manifests HorizontalPodAutoscaler "$name" '.spec.behavior.scaleDown // {} | "\(.stabilizationWindowSeconds) " + ([.policies // [] | .[] | "\(.type):\(.value)/\(.periodSeconds)"] | join(","))'
        assert_output "300 Percent:20/60"
    done
}

@test "the HPA owns the replica count" {
    for name in web api; do
        run get manifests Deployment "$name" '.spec.replicas // "unset"'
        assert_output "unset"
    done
    run get manifests StatefulSet db '.spec.replicas'
    assert_output "1"
}

@test "every autoscaled container requests what its metrics measure" {
    for name in web api; do
        run k8s manifests '. as $all | [$all[] | select(.kind == "HorizontalPodAutoscaler" and .metadata.name == $name) | .spec.metrics[] | select(.type == "Resource") | .resource.name] as $m
          | [$all[] | select(.kind == "Deployment" and .metadata.name == $name) | app_containers | . as $c | $m[] | select($c.resources.requests[.] == null) | "\($c.name) has no \(.) request"] | join(", ")' --arg name "$name"
        assert_output ""
    done
}
