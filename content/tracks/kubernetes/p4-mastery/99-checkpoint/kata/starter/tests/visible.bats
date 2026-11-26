load /opt/dojo/k8s

CRD="$BATS_TEST_DIRNAME/../chart/crds/databases.yaml"

@test "the CRD stores v1 and still serves v1alpha1" {
    run bash -c "yq -o=json '.' '$CRD' | jq -r '[.spec.versions[] | \"\(.name):served=\(.served),storage=\(.storage)\"] | sort | join(\" \")'"
    assert_output "v1:served=true,storage=true v1alpha1:served=true,storage=false"
}
