load /opt/dojo/k8s

@test "every prod object lives in catalog-prod" {
    run k8s prod '[.[] | .metadata.namespace // "-"] | unique | join(",")'
    assert_output "catalog-prod"
}
