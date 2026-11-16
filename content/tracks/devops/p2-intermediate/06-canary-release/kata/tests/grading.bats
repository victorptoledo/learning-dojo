load /opt/dojo/k8s

@test "the canary runs ghcr.io/acme/checkout:1.9.0, stable stays on 1.8.0 with 4 replicas" {
    run get manifests Deployment checkout-canary '.spec.template.spec.containers[] | select(.name == "checkout") | .image'
    assert_output "ghcr.io/acme/checkout:1.9.0"
    run get manifests Deployment checkout-stable '"\(.spec.replicas) \(.spec.template.spec.containers[0].image)"'
    assert_output "4 ghcr.io/acme/checkout:1.8.0"
}

@test "the canary's Pods are labelled app: checkout and track: canary" {
    run get manifests Deployment checkout-canary '.spec.template.metadata.labels | "\(.app) \(.track)"'
    assert_output "checkout canary"
}

@test "selectors, probes, resources and pinned images are right" {
    policy manifests selectors
    policy manifests service_targets
    policy manifests probes
    policy manifests resources
    policy manifests pinned_images
}

@test "each Service selects the Pods of exactly one track" {
    for track in stable canary; do
        run get manifests Service "checkout-$track" '.spec.selector | "\(.app) \(.track)"'
        assert_output "checkout $track"
    done
    stable=$(get manifests Deployment checkout-stable '.spec.template.metadata.labels | tojson')
    canary=$(get manifests Deployment checkout-canary '.spec.template.metadata.labels | tojson')
    for service in checkout-stable checkout-canary; do
        run get manifests Service "$service" '[matches(.spec.selector; $stable), matches(.spec.selector; $canary)] | map(select(.)) | length' \
            --argjson stable "$stable" --argjson canary "$canary"
        assert_output "1"
    done
}

@test "the route still serves shop.example.com through the Gateway public in infra" {
    run get manifests HTTPRoute checkout '"\(.spec.parentRefs[0].name) \(.spec.parentRefs[0].namespace) \(.spec.hostnames | join(","))"'
    assert_output "public infra shop.example.com"
}

@test "the first rule sends x-canary: always to the canary only" {
    run get manifests HTTPRoute checkout '.spec.rules[0] | "\(.matches[0].headers[0] | "\(.type // "Exact") \(.name) \(.value)") \([.backendRefs[] | "\(.name):\(.port)"] | join(","))"'
    assert_output "Exact x-canary always checkout-canary:80"
}

@test "every other request is split 90/10 between stable and canary" {
    run get manifests HTTPRoute checkout '.spec.rules[1] | "\(.matches[0].path.type) \(.matches[0].path.value)"'
    assert_output "PathPrefix /"
    run get manifests HTTPRoute checkout '.spec.rules[1].backendRefs | map("\(.name):\(.port):\(.weight // 1)") | sort | join(",")'
    assert_output "checkout-canary:80:10,checkout-stable:80:90"
}

@test "there are exactly two rules" {
    run get manifests HTTPRoute checkout '.spec.rules | length'
    assert_output "2"
}
