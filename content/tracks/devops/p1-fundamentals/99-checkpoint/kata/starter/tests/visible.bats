load /opt/dojo/devops

@test "the workflow has the jobs lint, test and image" {
    run wf ci.yml '.jobs | keys | join(",")'
    assert_output "image,lint,test"
}
