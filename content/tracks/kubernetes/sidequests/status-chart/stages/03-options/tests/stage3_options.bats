load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

setup_file() {
    helm_render shared-sa sp chart --set serviceAccount.create=false --set serviceAccount.name=status-readers
    helm_render no-sa sp chart --set serviceAccount.create=false
    helm_render named-sa sp chart --set serviceAccount.name=page-runner
    helm_render faster sp chart --set config.REFRESH_SECONDS=10 --set-string podAnnotations.team=sre
    helm_render scaled sp chart --set autoscaling.enabled=true --set autoscaling.minReplicas=3 --set autoscaling.maxReplicas=9 --set autoscaling.targetCPUUtilizationPercentage=60
    helm_render extra sp chart --set-json 'extraEnv=[{"name":"TZ","value":"Europe/Lisbon"},{"name":"NODE_NAME","valueFrom":{"fieldRef":{"fieldPath":"spec.nodeName"}}}]'
}

@test "every value set renders valid Kubernetes" {
    for target in shared-sa no-sa named-sa faster scaled extra; do
        valid "$target"
    done
}

@test "the chart creates a ServiceAccount that mounts no token" {
    run get default ServiceAccount sp-statuspage '"\(.automountServiceAccountToken) \(.metadata.labels["helm.sh/chart"])"'
    assert_output "false statuspage-0.1.0"
    run get default Deployment sp-statuspage '.spec.template.spec | "\(.serviceAccountName) \(.automountServiceAccountToken)"'
    assert_output "sp-statuspage false"
}

@test "a ServiceAccount can be named, shared or left to default" {
    run names named-sa ServiceAccount
    assert_output "page-runner"
    run get named-sa Deployment sp-statuspage '.spec.template.spec.serviceAccountName'
    assert_output "page-runner"
    run count shared-sa ServiceAccount
    assert_output "0"
    run get shared-sa Deployment sp-statuspage '.spec.template.spec.serviceAccountName'
    assert_output "status-readers"
    run get no-sa Deployment sp-statuspage '.spec.template.spec.serviceAccountName'
    assert_output "default"
}

@test "the settings are a ConfigMap of strings loaded by the container" {
    run get default ConfigMap sp-statuspage '.data | to_entries | map("\(.key)=\(.value | tojson)") | join(" ")'
    assert_output 'REFRESH_SECONDS="30" STATUS_TITLE="Acme status"'
    run container default sp-statuspage '[.envFrom // [] | .[] | .configMapRef.name] | join(" ")'
    assert_output "sp-statuspage"
    run get faster ConfigMap sp-statuspage '.data.REFRESH_SECONDS | tojson'
    assert_output '"10"'
    policy default config_refs
}

@test "a settings change rolls the Pods" {
    run get default Deployment sp-statuspage '.spec.template.metadata.annotations["checksum/config"] // ""'
    assert_output --regexp '^[0-9a-f]{64}$'
    default_sum=$output
    run get faster Deployment sp-statuspage '.spec.template.metadata.annotations["checksum/config"] // ""'
    assert_output --regexp '^[0-9a-f]{64}$'
    [ "$output" != "$default_sum" ] || fail "the checksum does not change when the settings change"
    run get shared-sa Deployment sp-statuspage '.spec.template.metadata.annotations["checksum/config"]'
    assert_output "$default_sum"
}

@test "pod annotations sit next to the checksum" {
    run get faster Deployment sp-statuspage '.spec.template.metadata.annotations | keys | join(" ")'
    assert_output "checksum/config team"
}

@test "there is no HPA by default" {
    run count default HorizontalPodAutoscaler
    assert_output "0"
}

@test "with autoscaling the HPA owns the replica count" {
    run get scaled HorizontalPodAutoscaler sp-statuspage '"\(.spec.scaleTargetRef.kind)/\(.spec.scaleTargetRef.name) \(.spec.minReplicas) \(.spec.maxReplicas)"'
    assert_output "Deployment/sp-statuspage 3 9"
    run get scaled HorizontalPodAutoscaler sp-statuspage '.spec.metrics[] | select(.type == "Resource" and .resource.name == "cpu") | .resource.target | "\(.type) \(.averageUtilization)"'
    assert_output "Utilization 60"
    run get scaled Deployment sp-statuspage '.spec.replicas // "unset"'
    assert_output "unset"
}

@test "a minimum above the maximum is refused" {
    run rejects sp chart --set autoscaling.enabled=true --set autoscaling.minReplicas=8 --set autoscaling.maxReplicas=4
    assert_success
    assert_output --partial "autoscaling.minReplicas must not be greater than autoscaling.maxReplicas"
}

@test "extra variables are added as they are" {
    run container extra sp-statuspage '.env // [] | map("\(.name)=\(.value // (.valueFrom | tojson))") | join(" ")'
    assert_output 'TZ=Europe/Lisbon NODE_NAME={"fieldRef":{"fieldPath":"spec.nodeName"}}'
    run container default sp-statuspage '.env // [] | length'
    assert_output "0"
}

@test "the schema knows the new values" {
    run rejects sp chart --set serviceAccount.craete=false
    assert_success
    assert_output --partial "craete"
    run rejects sp chart --set autoscaling.enabled=yes
    assert_success
    assert_output --partial "enabled"
    run rejects sp chart --set-json 'extraEnv=[{"value":"x"}]'
    assert_success
    assert_output --partial "name"
}
