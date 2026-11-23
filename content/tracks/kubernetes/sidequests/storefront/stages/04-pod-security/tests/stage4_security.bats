load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

workload() {
    k8s manifests '[.[] | select((.kind == "Deployment" or .kind == "StatefulSet") and .metadata.name == $name)] | first' --arg name "$1" -c
}

# writable NAME: the paths a workload's containers can write, sorted, one per line.
writable() {
    workload "$1" | jq -r '.spec.template.spec as $s
      | ([.spec.volumeClaimTemplates // [] | .[] | .metadata.name]) as $claims
      | [$s.containers[] | (.volumeMounts // [])[] | select(.readOnly != true) | . as $m
         | select(($claims | index($m.name)) != null or any($s.volumes // [] | .[]; .name == $m.name and .emptyDir != null))
         | .mountPath] | sort | .[]'
}

@test "every Pod meets the restricted Pod Security Standard" {
    policy manifests restricted
}

@test "every container has a read-only root" {
    policy manifests read_only_root
}

@test "the Pods run as fixed non-root users" {
    for pair in web:10001 api:10001 db:999; do
        name=${pair%%:*} uid=${pair#*:}
        run get manifests "$( [ "$name" = db ] && echo StatefulSet || echo Deployment)" "$name" '.spec.template.spec as $s | $s.containers[0].securityContext as $c
          | "\($c.runAsUser // $s.securityContext.runAsUser) \($c.runAsGroup // $s.securityContext.runAsGroup)"'
        assert_output "$uid $uid"
    done
}

@test "each part writes only where it must" {
    run writable web
    assert_output "/tmp"
    run writable api
    assert_output "/tmp"
    run writable db
    assert_output "$(printf '/tmp\n/var/lib/postgresql\n/var/run/postgresql')"
}

@test "every emptyDir has a size limit" {
    run k8s manifests '[.[] | select(is_workload) | ref as $r | pod_spec.volumes // [] | .[] | select(.emptyDir != null and .emptyDir.sizeLimit == null) | "\($r) volume \(.name)"] | join(", ")'
    assert_output ""
    run k8s manifests '[.[] | select(is_workload) | pod_spec.volumes // [] | .[] | select(.emptyDir != null) | .emptyDir.sizeLimit] | sort | unique | join(" ")'
    assert_output "16Mi 64Mi"
}

@test "the database volume belongs to the postgres group" {
    run get manifests StatefulSet db '.spec.template.spec.securityContext | "\(.fsGroup) \(.fsGroupChangePolicy)"'
    assert_output "999 OnRootMismatch"
}

@test "each part has its own ServiceAccount with no token" {
    run names manifests ServiceAccount
    for account in api db web; do
        assert_line "$account"
    done
    run k8s manifests '[.[] | select(.kind == "ServiceAccount" and .automountServiceAccountToken != false) | .metadata.name] | join(" ")'
    assert_output ""
    run k8s manifests '[.[] | select(.kind == "Deployment" or .kind == "StatefulSet") | "\(.metadata.name)=\(pod_spec.serviceAccountName // "default"):\(pod_spec.automountServiceAccountToken)"] | sort | join(" ")'
    assert_output "api=api:false db=db:false web=web:false"
}

@test "images keep their exact tags" {
    policy manifests pinned_images
}
