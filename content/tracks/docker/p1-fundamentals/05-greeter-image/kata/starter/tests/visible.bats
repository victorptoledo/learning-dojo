# The dojo runs your image once per line of the hidden tests/commands.txt, for example
# `docker run --rm IMAGE` (named "default") and keeps its output for cmd_out.
load /opt/dojo/facts

@test "greets the world by default" {
    run cmd_out default
    assert_output "hello, world!"
}
