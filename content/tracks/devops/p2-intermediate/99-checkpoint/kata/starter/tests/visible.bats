load /opt/dojo/devops

@test "the workflow has the jobs plan and apply" {
    run wf terraform.yml '.jobs | keys | join(",")'
    assert_output "apply,plan"
}
