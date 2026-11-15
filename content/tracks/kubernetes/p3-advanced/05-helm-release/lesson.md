# A Production Release with Helm

The `ledger` chart deploys fine with its defaults: one replica, no extras. Production needs
more: autoscaling on CPU, a disruption budget for node drains, a rollout that never loses
capacity, and Pods that restart when the configuration changes. Press **Enter** to create
the kata folder, then finish the templates in `chart/templates/` and write
`chart/values-prod.yaml`.
