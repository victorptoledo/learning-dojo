# The dojo builds your image, runs it once (`docker run --rm IMAGE`) and keeps what it saw.
# facts.bash has the helpers: cmd_out, image_jq, image_has_path, ...
load /opt/dojo/facts

@test "running the image prints hello.txt" {
    run cmd_out default
    assert_output "$(tr -d '\r' < hello.txt)"
}
