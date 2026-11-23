load /opt/dojo/k8s

@test "rollouts add a Pod before removing one" {
    for part in api web; do
        run get manifests Deployment "board-$part" '"\(.spec.strategy.type // "RollingUpdate") \(.spec.strategy.rollingUpdate.maxSurge) \(.spec.strategy.rollingUpdate.maxUnavailable)"'
        assert_output "RollingUpdate 1 0"
    done
}

@test "a new Pod must stay ready for 10 seconds and old revisions are kept" {
    for part in api web; do
        run get manifests Deployment "board-$part" '"\(.spec.minReadySeconds) \(.spec.progressDeadlineSeconds) \(.spec.revisionHistoryLimit)"'
        assert_output "10 300 5"
    done
}

@test "images name an exact version" {
    policy manifests pinned_images
}

@test "stopping Pods wait for the Service to forget them" {
    for part in api web; do
        run get manifests Deployment "board-$part" '.spec.template.spec.containers[0].lifecycle.preStop.sleep.seconds // "none"'
        assert_output "5"
    done
}

@test "the grace period covers the wait and the open requests" {
    for pair in api:30 web:10; do
        part=${pair%%:*} drain=${pair#*:}
        run get manifests Deployment "board-$part" '(.spec.template.spec.terminationGracePeriodSeconds // 30) - (.spec.template.spec.containers[0].lifecycle.preStop.sleep.seconds // 0)'
        [ "$output" -ge "$drain" ] || fail "board-$part leaves ${output}s after the preStop sleep, not $drain"
    done
}

@test "the API settings live in an immutable versioned ConfigMap" {
    run get manifests ConfigMap board-api-config-v1 '"\(.metadata.namespace) \(.immutable) \(.data.LOG_LEVEL) \(.data.MAX_NOTICE_LENGTH | tojson)"'
    assert_success
    assert_output 'board true info "500"'
    run get manifests ConfigMap board-api-config-v1 '.metadata.labels // {} | "\(.["app.kubernetes.io/name"]) \(.["app.kubernetes.io/part-of"])"'
    assert_output "board-api noticeboard"
}

@test "the API reads each setting by name from the ConfigMap" {
    for key in LOG_LEVEL MAX_NOTICE_LENGTH; do
        run get manifests Deployment board-api '[.spec.template.spec.containers[0].env // [] | .[] | select(.name == $key) | .valueFrom.configMapKeyRef // {} | "\(.name)/\(.key)"] | first // "missing"' --arg key "$key"
        assert_output "board-api-config-v1/$key"
    done
    policy manifests config_refs
}
