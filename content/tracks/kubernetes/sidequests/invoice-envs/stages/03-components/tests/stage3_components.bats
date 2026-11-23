load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

setup_file() {
    render dev overlays/dev
    render staging overlays/staging
    render prod overlays/prod
}

@test "the staging overlay builds and is valid Kubernetes" {
    valid staging
}

@test "staging runs two replicas of what production runs" {
    run k8s staging '[.[] | select(.metadata.namespace != "invoices-staging" or .metadata.labels.environment != "staging") | "\(.kind)/\(.metadata.name)"] | join(", ")'
    assert_output ""
    run get staging Deployment invoices '.spec.replicas'
    assert_output "2"
    prod_image=$(get prod Deployment invoices '.spec.template.spec.containers[0].image')
    run get staging Deployment invoices '.spec.template.spec.containers[0].image'
    assert_output "$prod_image"
    config=$(generated staging ConfigMap invoices-config)
    run get staging ConfigMap "$config" '.data.LOG_LEVEL'
    assert_output "info"
    run count staging Secret
    assert_output "0"
    run env_of staging
    assert_line 'DB_PASSWORD={"secretKeyRef":{"key":"password","name":"invoices-db"}}'
}

@test "dev has no public endpoint" {
    run count dev Ingress
    assert_output "0"
    run count dev PodDisruptionBudget
    assert_output "0"
}

@test "the public component lives in components/public" {
    run yq '.kind' "$KATA_DIR/components/public/kustomization.yaml"
    assert_output "Component"
    for env in staging prod; do
        run yq '.components // [] | map(sub("/$"; "")) | contains(["../../components/public"])' "$KATA_DIR/overlays/$env/kustomization.yaml"
        assert_output "true"
    done
}

@test "staging and prod are served on their own host with TLS" {
    for pair in staging:invoices.staging.acme.test prod:invoices.acme.test; do
        env=${pair%%:*} host=${pair#*:}
        run get "$env" Ingress invoices '"\(.spec.ingressClassName) " + ([.spec.rules[].host] | join(","))'
        assert_output "nginx $host"
        run get "$env" Ingress invoices '.spec.tls // [] | map("\(.secretName)=\(.hosts | join(","))") | join(" ")'
        assert_output "invoices-tls=$host"
    done
}

@test "the Ingress sends every path to the invoices Service port" {
    for env in staging prod; do
        run get "$env" Ingress invoices '.spec.rules[0].http.paths | map("\(.path) \(.pathType) \(.backend.service.name):\(.backend.service.port.name // .backend.service.port.number)") | join(" ")'
        assert_output --regexp '^/ Prefix invoices:(http|80)$'
        run get "$env" Service invoices '.spec.ports | map(.name, (.port | tostring)) | index("http") != null'
        assert_output "true"
    done
}

@test "a drain takes at most one invoices Pod at a time" {
    for env in staging prod; do
        run get "$env" PodDisruptionBudget invoices '"\(.spec.maxUnavailable) \(.spec.minAvailable // "none")"'
        assert_output "1 none"
        labels=$(get "$env" Deployment invoices '.spec.template.metadata.labels | tojson')
        run get "$env" PodDisruptionBudget invoices '(.spec.selector.matchLabels // {}) as $s | ($s | length) > 0 and matches($s; $labels)' --argjson labels "$labels"
        assert_output "true"
    done
}
