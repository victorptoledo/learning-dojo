load /opt/dojo/facts

@test "DATA_DIR is /data and /data is a volume of the image" {
    run image_env DATA_DIR
    assert_output "/data"
    run image_jq '.Config.Volumes // {} | has("/data")'
    assert_output "true"
}

@test "the running container keeps /data on a volume, not in its own layer" {
    run container_jq '.Mounts // [] | map(select(.Destination == "/data")) | .[0].Type // "none"'
    assert_output "volume"
}

@test "welcome.txt is installed in /usr/share/guestbook" {
    image_has_path /usr/share/guestbook/welcome.txt || fail "/usr/share/guestbook/welcome.txt is not in the image"
}

@test "the server keeps its messages in /data/messages.txt" {
    run container_logs
    assert_output --partial "messages in /data/messages.txt"
}

@test "the welcome message is the first message" {
    run http_get 8080 /messages
    assert_success
    assert_line --index 0 "dojo: welcome to the board, leave a message"
}

@test "a posted message is saved and listed" {
    run curl --silent --show-error --max-time 5 -X POST \
        --data-urlencode "name=ada" --data-urlencode "text=containers are processes" \
        --output /dev/null --write-out '%{http_code}' http://localhost:8080/messages
    assert_output "201"
    run http_get 8080 /messages
    assert_line "ada: containers are processes"
}
