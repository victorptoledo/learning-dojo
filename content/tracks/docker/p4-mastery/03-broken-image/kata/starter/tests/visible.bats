# What the dojo saw when it started your image once: container_status, container_health
# and container_logs (what `docker logs` printed).
load /opt/dojo/facts

@test "the container is still running" {
    run container_status
    assert_output "running"
}
