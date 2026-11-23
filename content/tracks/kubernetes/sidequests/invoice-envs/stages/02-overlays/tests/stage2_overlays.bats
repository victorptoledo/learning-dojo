load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

setup_file() {
    render dev overlays/dev
    render prod overlays/prod
}

@test "the dev overlay builds and is valid Kubernetes" {
    valid dev
}

@test "the prod overlay builds and is valid Kubernetes" {
    valid prod
}

@test "each overlay puts every object in its namespace" {
    for env in dev prod; do
        run k8s "$env" '[.[] | select(.metadata.namespace != $ns) | "\(.kind)/\(.metadata.name)"] | join(", ")' --arg ns "invoices-$env"
        assert_output ""
        run count "$env" Deployment
        assert_output "1"
    done
}

@test "dev runs one replica of the release candidate" {
    run get dev Deployment invoices '"\(.spec.replicas) \(.spec.template.spec.containers[0].image)"'
    assert_output "1 ghcr.io/acme/invoices:3.1.0-rc.2"
}

@test "prod runs three replicas of 3.0.4" {
    run get prod Deployment invoices '.spec.replicas'
    assert_output "3"
    run get prod Deployment invoices '.spec.template.spec.containers[0].image'
    assert_output --regexp '^ghcr\.io/acme/invoices:3\.0\.4(@sha256:[0-9a-f]{64})?$'
}

@test "each overlay changes only the log level of the generated settings" {
    for pair in dev:debug prod:warn; do
        env=${pair%%:*} level=${pair#*:}
        config=$(generated "$env" ConfigMap invoices-config)
        run get "$env" ConfigMap "$config" '"\(.data.LOG_LEVEL) \(.data.CURRENCY)"'
        assert_output "$level EUR"
        run env_of "$env"
        assert_line "envFrom=$config"
    done
    dev_config=$(generated dev ConfigMap invoices-config)
    prod_config=$(generated prod ConfigMap invoices-config)
    base_config=$(generated base ConfigMap invoices-config)
    [ "$dev_config" != "$base_config" ] && [ "$prod_config" != "$base_config" ] || fail "a merged ConfigMap kept the base's hash"
}

@test "dev generates its own database password" {
    secret=$(generated dev Secret invoices-db)
    [ "$secret" != "missing" ] || fail "dev has no generated invoices-db Secret"
    run get dev Secret "$secret" '.data.password | @base64d'
    assert_output "dev-only-password"
    run env_of dev
    assert_line "DB_PASSWORD={\"secretKeyRef\":{\"key\":\"password\",\"name\":\"$secret\"}}"
}

@test "prod carries no Secret and uses the one the platform creates" {
    run count prod Secret
    assert_output "0"
    run env_of prod
    assert_line 'DB_PASSWORD={"secretKeyRef":{"key":"password","name":"invoices-db"}}'
}

@test "prod requests 500m CPU and 512Mi memory with a 1Gi limit" {
    run get prod Deployment invoices '.spec.template.spec.containers[0].resources | "\(.requests.cpu) \(.requests.memory) \(.limits.memory)"'
    assert_output "500m 512Mi 1Gi"
    run get dev Deployment invoices '.spec.template.spec.containers[0].resources | "\(.requests.cpu) \(.requests.memory) \(.limits.memory)"'
    assert_output "100m 128Mi 256Mi"
}

@test "the environment label is on every object and Pod but never in a selector" {
    for env in dev prod; do
        run k8s "$env" '[.[] | select(.metadata.labels.environment != $env) | "\(.kind)/\(.metadata.name)"] | join(", ")' --arg env "$env"
        assert_output ""
        run get "$env" Deployment invoices '"\(.spec.template.metadata.labels.environment) \(.spec.selector.matchLabels | has("environment"))"'
        assert_output "$env false"
        run get "$env" Service invoices '.spec.selector | has("environment")'
        assert_output "false"
        policy "$env" selectors
        policy "$env" service_targets
    done
}
