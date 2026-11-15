load /opt/dojo/facts

@test "no API_TOKEN variable in the image" {
    run image_jq '.Config.Env // [] | map(select(startswith("API_TOKEN="))) | length'
    assert_output "0"
}
