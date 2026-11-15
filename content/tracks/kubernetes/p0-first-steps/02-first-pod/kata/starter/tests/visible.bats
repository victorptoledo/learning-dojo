load /opt/dojo/k8s

@test "runs busybox 1.37" {
    run get manifests Pod hello '.spec.containers[0].image'
    assert_output "busybox:1.37"
}
