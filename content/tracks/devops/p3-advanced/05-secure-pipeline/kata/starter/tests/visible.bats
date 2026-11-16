load /opt/dojo/devops

@test "pull requests are tested with pull_request, not pull_request_target" {
    run wf pr.yml '.on | keys | join(",")'
    assert_output "pull_request"
}
