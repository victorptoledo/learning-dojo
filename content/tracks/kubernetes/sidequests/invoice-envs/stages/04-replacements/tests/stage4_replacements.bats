load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

DIGEST=sha256:3b5a1f7e0c9d2a4b6c8e0f1a3b5c7d9e1f2a4b6c8d0e2f4a6b8c0d2e4f6a8b0c

setup_file() {
    render dev overlays/dev
    render staging overlays/staging
    render prod overlays/prod
}

public_url() {
    get "$1" ConfigMap "$(generated "$1" ConfigMap invoices-config)" '.data.PUBLIC_URL'
}

@test "PUBLIC_URL follows the Ingress host" {
    for pair in staging:invoices.staging.acme.test prod:invoices.acme.test; do
        env=${pair%%:*} host=${pair#*:}
        run public_url "$env"
        assert_output "https://$host"
    done
    run public_url dev
    assert_output "http://localhost:8080"
}

@test "staging and prod settings have different hashes" {
    staging=$(generated staging ConfigMap invoices-config)
    prod=$(generated prod ConfigMap invoices-config)
    [ "$staging" != "$prod" ] || fail "both environments use $staging"
}

@test "each public host is written once in its overlay" {
    for pair in staging:invoices.staging.acme.test prod:invoices.acme.test; do
        env=${pair%%:*} host=${pair#*:}
        run bash -c "grep -rhoF '$host' '$KATA_DIR/overlays/$env' | wc -l"
        assert_output "1"
        run yq '[.replacements // [] | .[] | select(.source.kind == "Ingress")] | length > 0' "$KATA_DIR/overlays/$env/kustomization.yaml"
        assert_output "true"
    done
}

@test "staging and prod pin the image by digest" {
    for env in staging prod; do
        run get "$env" Deployment invoices '.spec.template.spec.containers[0].image'
        assert_output "ghcr.io/acme/invoices:3.0.4@$DIGEST"
    done
}

@test "nothing from dev reaches staging or prod" {
    for env in staging prod; do
        run grep -nE -e '-rc' -e 'debug' -e 'dev-only' -e 'invoices-dev' "$RENDERED/$env.yaml"
        assert_output ""
        run count "$env" Secret
        assert_output "0"
    done
}
