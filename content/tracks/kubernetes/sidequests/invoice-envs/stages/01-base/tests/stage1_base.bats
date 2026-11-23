load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

@test "the base renders the Deployment, the Service and two generated ConfigMaps" {
    run names base Deployment
    assert_output "invoices"
    run names base Service
    assert_output "invoices"
    run generated base ConfigMap invoices-config
    refute_output "missing"
    run generated base ConfigMap invoices-rates
    refute_output "missing"
    run count base Secret
    assert_output "0"
}

@test "every object and selector carries the common labels" {
    run k8s base '[.[] | select((.metadata.labels // {}) | .["app.kubernetes.io/name"] != "invoices" or .["app.kubernetes.io/part-of"] != "billing") | "\(.kind)/\(.metadata.name)"] | join(", ")'
    assert_output ""
    run get base Deployment invoices '.spec.selector.matchLabels | "\(.["app.kubernetes.io/name"]) \(.["app.kubernetes.io/part-of"])"'
    assert_output "invoices billing"
    run get base Service invoices '.spec.selector // {} | "\(.["app.kubernetes.io/name"]) \(.["app.kubernetes.io/part-of"])"'
    assert_output "invoices billing"
    policy base selectors
    policy base service_targets
}

@test "the settings come from config.env through a generated ConfigMap" {
    config=$(generated base ConfigMap invoices-config)
    run get base ConfigMap "$config" '.data | to_entries | map("\(.key)=\(.value)") | sort | join(" ")'
    assert_output "CURRENCY=EUR LOG_LEVEL=info PUBLIC_URL=http://localhost:8080"
    run env_of base
    assert_line "envFrom=$config"
    refute_line --regexp '^(LOG_LEVEL|CURRENCY|PUBLIC_URL)='
    policy base config_refs
}

@test "the tax rates are mounted read-only from a generated ConfigMap" {
    rates=$(generated base ConfigMap invoices-rates)
    run get base ConfigMap "$rates" '.data | keys | join(" ")'
    assert_output "rates.yaml"
    run get base ConfigMap "$rates" '.data["rates.yaml"]'
    assert_output --partial "PT: 23"
    run get base Deployment invoices '.spec.template.spec as $s | $s.containers[0].volumeMounts // [] | .[]
      | select(.mountPath == "/etc/invoices") | .name as $v
      | "\(.readOnly) " + ([$s.volumes // [] | .[] | select(.name == $v) | .configMap.name] | first // "none")'
    assert_output "true $rates"
}

@test "the password comes from the invoices-db Secret, not the base" {
    run env_of base
    assert_line 'DB_PASSWORD={"secretKeyRef":{"key":"password","name":"invoices-db"}}'
}
