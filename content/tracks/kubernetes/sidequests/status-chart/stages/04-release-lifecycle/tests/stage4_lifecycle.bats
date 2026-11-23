load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

setup_file() {
    helm_render no-migrations sp chart --set migrations.enabled=false
    helm_render custom-port sp chart --set service.port=8000 --set image.tag=2.4.0
}

# notes ARGS...: the release notes of `helm install ARGS... --dry-run=client`.
notes() {
    (cd "$KATA_DIR" && helm install "$@" --dry-run=client) 2>&1 | sed -n '/^NOTES:/,$p'
}

annotation() {
    get "$1" "$2" "$3" '.metadata.annotations[$key] // "none"' --arg key "$4"
}

@test "the migration runs before install and upgrade, first among hooks" {
    run annotation default Job sp-statuspage-migrate helm.sh/hook
    assert_output "pre-install,pre-upgrade"
    run annotation default Job sp-statuspage-migrate helm.sh/hook-weight
    assert_output "-5"
    run annotation default Job sp-statuspage-migrate helm.sh/hook-delete-policy
    assert_output "before-hook-creation,hook-succeeded"
}

@test "the migration Job runs the page's image once, with a deadline" {
    run get default Job sp-statuspage-migrate '"\(.spec.backoffLimit) \(.spec.activeDeadlineSeconds) \(.spec.template.spec.restartPolicy)"'
    assert_output "1 300 Never"
    run get default Job sp-statuspage-migrate '.spec.template.spec.containers[0] | "\(.image) \(.args | tojson)"'
    assert_output 'ghcr.io/acme/statuspage:2.3.0 ["migrate"]'
    run get custom-port Job sp-statuspage-migrate '.spec.template.spec.containers[0].image'
    assert_output "ghcr.io/acme/statuspage:2.4.0"
    policy default resources
}

@test "the migration uses nothing the release has not created yet" {
    run get default Job sp-statuspage-migrate '.spec.template.spec | "\(.serviceAccountName // "default") \(.automountServiceAccountToken)"'
    assert_output "default false"
    run get default Job sp-statuspage-migrate '[.spec.template.spec.containers[] | .envFrom // [] | .[]] | length'
    assert_output "0"
    run get default Job sp-statuspage-migrate '[.. | objects | .configMapKeyRef?, .configMap?, .secretKeyRef? | select(. != null)] | length'
    assert_output "0"
}

@test "migrations can be switched off" {
    valid no-migrations
    run count no-migrations Job
    assert_output "0"
    run rejects sp chart --set migrations.enabeld=false
    assert_success
    assert_output --partial "enabeld"
}

@test "helm test checks the Service from inside the cluster" {
    run annotation default Pod sp-statuspage-test helm.sh/hook
    assert_output "test"
    run get default Pod sp-statuspage-test '"\(.spec.restartPolicy) \(.spec.containers[0].image)"'
    assert_output "Never busybox:1.37.0"
    run get default Pod sp-statuspage-test '.spec.containers[0] | (.command + (.args // [])) | join(" ")'
    assert_output "wget -qO- http://sp-statuspage:80/health"
    run get custom-port Pod sp-statuspage-test '.spec.containers[0] | (.command + (.args // [])) | join(" ")'
    assert_output "wget -qO- http://sp-statuspage:8000/health"
    run get default Pod sp-statuspage-test '.spec.containers[0].resources | "\(.requests.cpu) \(.requests.memory) \(.limits.memory)"'
    assert_output "10m 16Mi 32Mi"
}

@test "the notes say how to reach the page without an Ingress" {
    run notes sp chart --namespace status
    assert_line --partial "kubectl --namespace status port-forward svc/sp-statuspage 8080:80"
    assert_line --partial "Status page: http://localhost:8080"
}

@test "the notes give the public URL with an Ingress" {
    run notes sp chart --set ingress.enabled=true --set ingress.host=status.acme.test --set ingress.tls.enabled=true --set ingress.tls.secretName=status-tls
    assert_line --partial "Status page: https://status.acme.test"
    refute_line --partial "port-forward"
    run notes sp chart --set ingress.enabled=true --set ingress.host=status.acme.test
    assert_line --partial "Status page: http://status.acme.test"
}

@test "the chart passes a strict lint" {
    run bash -c "cd '$KATA_DIR' && helm lint --strict chart"
    assert_success
    run bash -c "cd '$KATA_DIR' && helm lint --strict chart --set ingress.enabled=true --set ingress.host=status.acme.test"
    assert_success
}
