load /opt/dojo/k8s

@test "the postgres Service is headless" {
    run get manifests Service postgres '.spec.clusterIP'
    assert_output "None"
}
