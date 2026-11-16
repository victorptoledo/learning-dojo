load /opt/dojo/k8s

@test "there is a canary Deployment with one replica" {
    run get manifests Deployment checkout-canary '.spec.replicas'
    assert_output "1"
}
