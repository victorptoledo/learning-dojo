load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

setup_file() {
    helm_render public sp chart --set ingress.enabled=true --set ingress.host=status.acme.test
    helm_render public-tls sp chart --set ingress.enabled=true --set ingress.host=status.acme.test \
        --set ingress.tls.enabled=true --set ingress.tls.secretName=status-tls
}

@test "the chart has a values schema" {
    [ -f "$KATA_DIR/chart/values.schema.json" ] || fail "chart/values.schema.json is missing"
    run jq -e 'type == "object"' "$KATA_DIR/chart/values.schema.json"
    assert_success
}

@test "a misspelled value is an error, not ignored" {
    run rejects sp chart --set replicacount=3
    assert_success
    assert_output --partial "replicacount"
    run rejects sp chart --set image.tga=2.4.0
    assert_success
    assert_output --partial "tga"
}

@test "the replica count is a positive integer" {
    run rejects sp chart --set replicaCount=0
    assert_success
    assert_output --partial "replicaCount"
    run rejects sp chart --set replicaCount=two
    assert_success
    assert_output --partial "replicaCount"
}

@test "the pull policy and the Service type are one of the known ones" {
    run rejects sp chart --set image.pullPolicy=Sometimes
    assert_success
    assert_output --partial "pullPolicy"
    run rejects sp chart --set service.type=ExternalName
    assert_success
    assert_output --partial "type"
}

@test "the Service port is a number from 1 to 65535" {
    run rejects sp chart --set service.port=http
    assert_success
    assert_output --partial "port"
    run rejects sp chart --set service.port=70000
    assert_success
    assert_output --partial "port"
}

@test "the image repository cannot be empty" {
    run rejects sp chart --set image.repository=
    assert_success
    assert_output --partial "repository"
}

@test "good values still render" {
    run bash -c "cd '$KATA_DIR' && helm template sp chart --set replicaCount=4 --set service.port=8443 --set image.pullPolicy=Always > /dev/null"
    assert_success
}

@test "the Ingress is off by default" {
    run count default Ingress
    assert_output "0"
    run count named Ingress
    assert_output "0"
}

@test "an enabled Ingress routes its host to the release's Service" {
    valid public
    run get public Ingress sp-statuspage '"\(.spec.ingressClassName) \(.spec.rules[0].host) \(.spec.tls // "no-tls")"'
    assert_output "nginx status.acme.test no-tls"
    run get public Ingress sp-statuspage '.spec.rules[0].http.paths | map("\(.path) \(.pathType) \(.backend.service.name):\(.backend.service.port.name)") | join(" ")'
    assert_output "/ Prefix sp-statuspage:http"
    run get public Ingress sp-statuspage '.metadata.labels["helm.sh/chart"]'
    assert_output "statuspage-0.1.0"
}

@test "TLS uses the host and the Secret from the values" {
    valid public-tls
    run get public-tls Ingress sp-statuspage '.spec.tls | map("\(.secretName)=\(.hosts | join(","))") | join(" ")'
    assert_output "status-tls=status.acme.test"
}

@test "an enabled Ingress needs a host, and TLS needs its Secret" {
    run rejects sp chart --set ingress.enabled=true
    assert_success
    assert_output --partial "ingress.host is required when ingress.enabled is true"
    run rejects sp chart --set ingress.enabled=true --set ingress.host=status.acme.test --set ingress.tls.enabled=true
    assert_success
    assert_output --partial "ingress.tls.secretName is required when ingress.tls.enabled is true"
}
