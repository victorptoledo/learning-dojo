load /opt/dojo/k8s

@test "the Service reaches the greeter Pods on a declared port" {
    policy manifests service_targets
}
