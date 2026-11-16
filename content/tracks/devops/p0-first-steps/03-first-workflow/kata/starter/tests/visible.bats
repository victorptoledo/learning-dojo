load /opt/dojo/devops

@test "the workflow is named ci" {
    run wf ci.yml '.name'
    assert_output "ci"
}
