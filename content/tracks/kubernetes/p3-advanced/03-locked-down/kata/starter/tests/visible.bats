load /opt/dojo/k8s

@test "the Pods meet the restricted Pod Security Standard" {
    policy manifests restricted
}
