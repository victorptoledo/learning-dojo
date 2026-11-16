load /opt/dojo/devops

@test "the module declares the outputs name, tags and config_files" {
    run tf_outputs .
    assert_output "config_files
name
tags"
}

@test "it pins Terraform 1.9+ and hashicorp/local ~> 2.9" {
    run terraform providers -no-color
    assert_success
    assert_output --partial "provider[registry.terraform.io/hashicorp/local] ~> 2.9"
    run grep -E 'required_version[[:space:]]*=[[:space:]]*">= ?1\.9"' versions.tf
    assert_success
}
