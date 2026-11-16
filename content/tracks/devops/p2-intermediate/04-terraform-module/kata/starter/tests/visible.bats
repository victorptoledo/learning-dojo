load /opt/dojo/devops

@test "the module declares its five variables" {
    run tf_variables .
    assert_output "environment
extra_tags
regions
replicas
service"
}
