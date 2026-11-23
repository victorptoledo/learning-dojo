load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

@test "the shop still has its three parts and their Services" {
    run k8s manifests '[.[] | select(.kind == "Deployment" or .kind == "StatefulSet") | "\(.kind)/\(.metadata.name)"] | sort | join(" ")'
    assert_output "Deployment/api Deployment/web StatefulSet/db"
    run names manifests Service
    assert_output "$(printf 'api\ndb\nweb')"
    policy manifests selectors
    policy manifests service_targets
}

@test "a default deny selects every Pod for ingress and egress" {
    run k8s manifests '[.[] | select(.kind == "NetworkPolicy" and .metadata.namespace == "shop")
      | select((.spec.podSelector // {}) == {} or ((.spec.podSelector.matchLabels // {}) == {} and (.spec.podSelector.matchExpressions // []) == []))
      | select((.spec.policyTypes // []) | (index("Ingress") != null and index("Egress") != null))
      | select((.spec.ingress // []) == [] and (.spec.egress // []) == [])] | length > 0'
    assert_output "true"
}

@test "the ingress controller reaches web and nothing else" {
    expect_reach ingress web TCP 8080 allowed
    expect_reach ingress api TCP 8080 denied
    expect_reach ingress db TCP 5432 denied
    expect_reach ingress web TCP 9090 denied
}

@test "web reaches api on 8080 only" {
    expect_reach web api TCP 8080 allowed
    expect_reach web api TCP 9090 denied
    expect_reach web api UDP 8080 denied
}

@test "only api reaches the database" {
    expect_reach api db TCP 5432 allowed
    expect_reach web db TCP 5432 denied
    expect_reach db db TCP 5432 denied
    expect_reach api db TCP 22 denied
}

@test "nothing calls back upstream" {
    expect_reach api web TCP 8080 denied
    expect_reach db api TCP 8080 denied
    expect_reach db web TCP 8080 denied
}

@test "a Pod in another namespace with web's labels gets nowhere" {
    expect_reach stranger api TCP 8080 denied
    expect_reach stranger web TCP 8080 denied
    expect_reach stranger db TCP 5432 denied
}

@test "every Pod resolves names through CoreDNS" {
    for pod in web api db; do
        expect_reach "$pod" dns UDP 53 allowed
        expect_reach "$pod" dns TCP 53 allowed
        expect_reach "$pod" dns TCP 8080 denied
    done
}

@test "api reaches the payment provider on 443 only" {
    expect_reach api internet TCP 443 allowed
    expect_reach api internet TCP 80 denied
}

@test "api cannot reach private or metadata addresses" {
    expect_reach api metadata TCP 80 denied
    expect_reach api metadata TCP 443 denied
    expect_reach api node TCP 443 denied
}

@test "web and db never reach the internet" {
    for pod in web db; do
        expect_reach "$pod" internet TCP 443 denied
        expect_reach "$pod" metadata TCP 80 denied
    done
}
