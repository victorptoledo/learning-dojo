load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

setup_file() {
    helm_render override sp chart --set fullnameOverride=board-status
    # 53 characters, the longest release name Helm accepts: with -statuspage it is 64.
    helm_render long status-page-for-the-platform-team-in-the-eu-west-zone chart
    # Cut at 63, this override would end with a dash.
    helm_render dash sp chart --set fullnameOverride="$(printf 'a%.0s' $(seq 62))-tail"
    helm_render tagged sp chart --set image.tag=2.4.0-beta.1 --set replicaCount=5 --set service.type=NodePort --set service.port=8000
}

@test "a release is named <release>-statuspage" {
    run names default Deployment
    assert_output "sp-statuspage"
    run names default Service
    assert_output "sp-statuspage"
}

@test "a release named after the chart is not named twice" {
    run names named Deployment
    assert_output "statuspage"
    run names named Service
    assert_output "statuspage"
}

@test "fullnameOverride names everything" {
    valid override
    run names override Deployment
    assert_output "board-status"
    run names override Service
    assert_output "board-status"
}

@test "long release names are cut to 63 characters with no trailing dash" {
    valid long
    run names long Deployment
    assert_output "status-page-for-the-platform-team-in-the-eu-west-zone-statuspag"
    valid dash
    run names dash Service
    assert_output "$(printf 'a%.0s' $(seq 62))"
}

@test "every object carries the chart labels" {
    run k8s default '[.[] | .metadata.labels // {} | "\(.["helm.sh/chart"]) \(.["app.kubernetes.io/name"]) \(.["app.kubernetes.io/instance"]) \(.["app.kubernetes.io/version"]) \(.["app.kubernetes.io/managed-by"])"] | unique | join("|")'
    assert_output "statuspage-0.1.0 statuspage sp 2.3.0 Helm"
}

@test "selectors use only the name and the instance" {
    run get default Deployment sp-statuspage '.spec.selector.matchLabels | to_entries | map("\(.key)=\(.value)") | sort | join(" ")'
    assert_output "app.kubernetes.io/instance=sp app.kubernetes.io/name=statuspage"
    run get default Service sp-statuspage '.spec.selector | to_entries | map("\(.key)=\(.value)") | sort | join(" ")'
    assert_output "app.kubernetes.io/instance=sp app.kubernetes.io/name=statuspage"
    for target in default named; do
        policy "$target" selectors
        policy "$target" service_targets
    done
}

@test "two releases never select each other's Pods" {
    sp_selector=$(get default Service sp-statuspage '.spec.selector | tojson')
    run get named Deployment statuspage 'matches($s; .spec.template.metadata.labels)' --argjson s "$sp_selector"
    assert_output "false"
}

@test "the defaults run 2 replicas of the app version" {
    run get default Deployment sp-statuspage '.spec.replicas'
    assert_output "2"
    run container default sp-statuspage '"\(.image) \(.imagePullPolicy)"'
    assert_output "ghcr.io/acme/statuspage:2.3.0 IfNotPresent"
    run get default Service sp-statuspage '"\(.spec.type) " + (.spec.ports | map("\(.port)->\(.targetPort)") | join(" "))'
    assert_output "ClusterIP 80->http"
}

@test "values change the tag, the replicas and the Service" {
    valid tagged
    run get tagged Deployment sp-statuspage '.spec.replicas'
    assert_output "5"
    run container tagged sp-statuspage '.image'
    assert_output "ghcr.io/acme/statuspage:2.4.0-beta.1"
    run get tagged Service sp-statuspage '"\(.spec.type) " + (.spec.ports | map("\(.port)->\(.targetPort)") | join(" "))'
    assert_output "NodePort 8000->http"
}

@test "the container has its port, probes and resources from the values" {
    run container default sp-statuspage '(.ports | map("\(.name)=\(.containerPort)") | join(" ")) + " \(.readinessProbe.httpGet.path) \(.livenessProbe.httpGet.path)"'
    assert_output "http=8080 /ready /health"
    run container default sp-statuspage '.resources | "\(.requests.cpu) \(.requests.memory) \(.limits.memory)"'
    assert_output "50m 64Mi 128Mi"
    policy default probes
    policy default resources
    policy default probe_ports
}
