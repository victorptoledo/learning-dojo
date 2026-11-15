load /opt/dojo/k8s

@test "declares exactly one Pod named hello" {
    run count manifests Pod
    assert_output "1"
    obj manifests Pod hello > /dev/null
}

@test "labels the Pod app=hello and track=kubernetes" {
    run get manifests Pod hello '.metadata.labels.app'
    assert_output "hello"
    run get manifests Pod hello '.metadata.labels.track'
    assert_output "kubernetes"
}

@test "has one container named hello" {
    run get manifests Pod hello '[.spec.containers[].name] | join(",")'
    assert_output "hello"
}

@test "pins the image to a version" {
    policy manifests pinned_images
}

@test "runs echo hello, dojo" {
    run get manifests Pod hello '.spec.containers[0].command // [] | tojson'
    assert_output '["echo","hello, dojo"]'
}

@test "is not restarted after it exits" {
    run get manifests Pod hello '.spec.restartPolicy'
    assert_output "Never"
}
