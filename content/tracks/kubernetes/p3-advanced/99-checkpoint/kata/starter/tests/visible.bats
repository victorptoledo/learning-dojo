load /opt/dojo/k8s

@test "the worker Pods run as their own ServiceAccount" {
    run names default ServiceAccount
    assert_output "thumbnailer"
    run get default Deployment thumbnailer '.spec.template.spec.serviceAccountName // "default"'
    assert_output "thumbnailer"
}
