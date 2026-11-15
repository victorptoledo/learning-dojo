load /opt/dojo/k8s

@test "every configuration reference resolves" {
    policy manifests config_refs
}
