load /opt/dojo/k8s

@test "prod creates a HorizontalPodAutoscaler for the ledger Deployment" {
    run get prod HorizontalPodAutoscaler ledger '.spec.scaleTargetRef | "\(.kind)/\(.name)"'
    assert_output "Deployment/ledger"
}
