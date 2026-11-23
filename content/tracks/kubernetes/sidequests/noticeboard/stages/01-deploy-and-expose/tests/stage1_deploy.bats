load /opt/dojo/k8s

# The Pod labels of a Deployment, as JSON.
pod_labels_of() {
    get manifests Deployment "$1" '.spec.template.metadata.labels // {} | tojson'
}

@test "the board namespace holds every other object" {
    run get manifests Namespace board '.metadata.labels["app.kubernetes.io/part-of"] // ""'
    assert_success
    assert_output "noticeboard"
    run k8s manifests '[.[] | select(.kind != "Namespace" and .metadata.namespace != "board") | "\(.kind)/\(.metadata.name)"] | join(", ")'
    assert_output ""
}

@test "the API runs 2 replicas of board-api 2.4.1 on the port named http" {
    run get manifests Deployment board-api '"\(.spec.replicas) \(.spec.template.spec.containers | length)"'
    assert_output "2 1"
    run get manifests Deployment board-api '.spec.template.spec.containers[0] | "\(.name) \(.image)"'
    assert_output "api ghcr.io/acme/board-api:2.4.1"
    run get manifests Deployment board-api '.spec.template.spec.containers[0].ports // [] | map("\(.name)=\(.containerPort)") | join(" ")'
    assert_output "http=8080"
}

@test "the web part runs 2 replicas of board-web 1.9.0 on the port named http" {
    run get manifests Deployment board-web '"\(.spec.replicas) \(.spec.template.spec.containers | length)"'
    assert_output "2 1"
    run get manifests Deployment board-web '.spec.template.spec.containers[0] | "\(.name) \(.image)"'
    assert_output "web ghcr.io/acme/board-web:1.9.0"
    run get manifests Deployment board-web '.spec.template.spec.containers[0].ports // [] | map("\(.name)=\(.containerPort)") | join(" ")'
    assert_output "http=3000"
}

@test "every object and Pod template carries the recommended labels" {
    for part in api web; do
        for kind in Deployment Service; do
            run get manifests "$kind" "board-$part" '.metadata.labels // {} | "\(.["app.kubernetes.io/name"]) \(.["app.kubernetes.io/component"]) \(.["app.kubernetes.io/part-of"])"'
            assert_output "board-$part $part noticeboard"
        done
        run get manifests Deployment "board-$part" '.spec.template.metadata.labels // {} | "\(.["app.kubernetes.io/name"]) \(.["app.kubernetes.io/component"]) \(.["app.kubernetes.io/part-of"])"'
        assert_output "board-$part $part noticeboard"
    done
}

@test "the version label follows the image tag" {
    for part in api web; do
        tag=$(get manifests Deployment "board-$part" '.spec.template.spec.containers[0].image | split(":") | last')
        run get manifests Deployment "board-$part" '"\(.metadata.labels["app.kubernetes.io/version"]) \(.spec.template.metadata.labels["app.kubernetes.io/version"])"'
        assert_output "$tag $tag"
    done
}

@test "selectors match their own Pods and never the version" {
    policy manifests selectors
    for part in api web; do
        run get manifests Deployment "board-$part" '.spec.selector.matchLabels | has("app.kubernetes.io/version")'
        assert_output "false"
        run get manifests Service "board-$part" '.spec.selector // {} | has("app.kubernetes.io/version")'
        assert_output "false"
    done
}

@test "each Service reaches the Pods of one Deployment on the named port http" {
    policy manifests service_targets
    for part in api web; do
        run get manifests Service "board-$part" '"\(.spec.type // "ClusterIP") " + (.spec.ports // [] | map("\(.name):\(.port)->\(.targetPort)") | join(" "))'
        assert_output "ClusterIP http:80->http"
        selector=$(get manifests Service "board-$part" '.spec.selector // {} | tojson')
        for other in api web; do
            labels=$(pod_labels_of "board-$other")
            run jq -n --argjson s "$selector" --argjson l "$labels" '$s | to_entries | all(.value == ($l[.key] // null))'
            if [ "$other" = "$part" ]; then assert_output "true"; else assert_output "false"; fi
        done
    done
}

@test "the web part finds the API by its Service DNS name" {
    run get manifests Deployment board-web '[.spec.template.spec.containers[0].env // [] | .[] | select(.name == "API_URL") | .value] | first // "missing"'
    assert_output --regexp '^http://board-api(\.board(\.svc(\.cluster\.local)?)?)?(:80)?/?$'
}
