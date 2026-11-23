# Fakes for the tests of stage 2 and later, first in PATH (sourced after helpers.sh):
#   kubectl  keeps each object as $KUBE_STATE/KIND-NAME.json in namespace $KUBE_NAMESPACE,
#            supports get -o json, patch (--type=json or merge), scale --replicas, and
#            records every call in $KUBE_LOG and the route's weights after every patch
#            in $WEIGHTS_LOG ("STABLE CANARY")
#   promq    answers line N of $PROMQ_ANSWERS on its Nth call (the last line after that):
#            a number, "NaN", "empty" (prints nothing) or "error" (fails)
#   sleep    records its argument in $SLEEP_LOG and returns at once, except its
#            $SLEEP_HOLD_AT-th call, which touches $SLEEP_HELD and waits while
#            $SLEEP_GATE exists

fakebin="$BATS_TEST_TMPDIR/fakebin"
KUBE_STATE="$BATS_TEST_TMPDIR/cluster"
KUBE_NAMESPACE=shop
KUBE_LOG="$BATS_TEST_TMPDIR/kubectl.log"
WEIGHTS_LOG="$BATS_TEST_TMPDIR/weights.log"
PROMQ_ANSWERS="$BATS_TEST_TMPDIR/promq.answers"
PROMQ_LOG="$BATS_TEST_TMPDIR/promq.log"
SLEEP_LOG="$BATS_TEST_TMPDIR/sleep.log"
SLEEP_HELD="$BATS_TEST_TMPDIR/sleep.held"
SLEEP_GATE="$BATS_TEST_TMPDIR/sleep.gate"
STABLE=checkout-stable
CANARY=checkout-canary
export KUBE_STATE KUBE_NAMESPACE KUBE_LOG WEIGHTS_LOG PROMQ_ANSWERS PROMQ_LOG SLEEP_LOG \
    SLEEP_HELD SLEEP_GATE STABLE CANARY
mkdir -p "$fakebin" "$KUBE_STATE"
: > "$KUBE_LOG"
: > "$WEIGHTS_LOG"
: > "$PROMQ_LOG"
: > "$SLEEP_LOG"
echo 0 > "$PROMQ_ANSWERS"

cat > "$fakebin/kubectl" <<'KUBECTL'
#!/usr/bin/env bash
jq -cn '$ARGS.positional' --args -- "$@" >> "$KUBE_LOG"
ns=default type=strategic patch='' output='' replicas=''
pos=()
while [ "$#" -gt 0 ]; do
    case "$1" in
        -n | --namespace) ns=$2; shift 2 ;;
        --namespace=*) ns=${1#*=}; shift ;;
        -o | --output) output=$2; shift 2 ;;
        -o=* | --output=*) output=${1#*=}; shift ;;
        -ojson) output=json; shift ;;
        --type) type=$2; shift 2 ;;
        --type=*) type=${1#*=}; shift ;;
        -p | --patch) patch=$2; shift 2 ;;
        -p=* | --patch=*) patch=${1#*=}; shift ;;
        --replicas) replicas=$2; shift 2 ;;
        --replicas=*) replicas=${1#*=}; shift ;;
        -*) echo "fake kubectl: unsupported flag $1" >&2; exit 1 ;;
        *) pos+=("$1"); shift ;;
    esac
done
if [ -n "${KUBE_EXPECT_CONFIG:-}" ] && { [ ! -f "${KUBECONFIG:-}" ] || [ "$(cat "$KUBECONFIG")" != "$KUBE_EXPECT_CONFIG" ]; }; then
    echo "error: no cluster configured (KUBECONFIG=${KUBECONFIG:-})" >&2
    exit 1
fi
verb=${pos[0]:-} kind=${pos[1]:-} name=${pos[2]:-}
# set image KIND/NAME CONTAINER=IMAGE
if [ "$verb" = set ] && [ "$kind" = image ]; then
    verb="set image" kind=${pos[2]:-} name=''
    image=${pos[3]:-}
fi
if [[ $kind == */* ]]; then
    name=${kind#*/}
    kind=${kind%%/*}
fi
case "$kind" in
    httproute | httproutes | httproute.gateway.networking.k8s.io) kind=httproute ;;
    deployment | deployments | deploy | deployment.apps) kind=deployment ;;
esac
if [ -n "${KUBE_FAIL:-}" ] && [ "$verb" = "$KUBE_FAIL" ]; then
    echo "error: the server is unavailable (fake kubectl, $verb)" >&2
    exit 1
fi
file="$KUBE_STATE/$kind-$name.json"
if [ "$ns" != "$KUBE_NAMESPACE" ] || [ ! -f "$file" ]; then
    echo "Error from server (NotFound): $kind \"$name\" not found in namespace \"$ns\"" >&2
    exit 1
fi
case "$verb" in
    get)
        [ "$output" = json ] || { echo "fake kubectl: only get -o json is supported" >&2; exit 1; }
        cat "$file"
        ;;
    patch)
        case "$type" in
            json)
                jq --argjson ops "$patch" 'reduce $ops[] as $op (.;
                    ($op.path | ltrimstr("/") | split("/") | map(if test("^[0-9]+$") then tonumber else . end)) as $p
                    | if $op.op == "replace" then
                        (if getpath($p) == null then error("replace: no value at \($op.path)") else setpath($p; $op.value) end)
                      elif $op.op == "add" then setpath($p; $op.value)
                      elif $op.op == "test" then (if getpath($p) == $op.value then . else error("test failed at \($op.path)") end)
                      else error("unsupported op \($op.op)") end)' "$file" > "$file.new" || exit 1
                ;;
            merge) jq --argjson p "$patch" '. * $p' "$file" > "$file.new" || exit 1 ;;
            *) echo "fake kubectl: an HTTPRoute is a custom resource: use --type=json or --type=merge" >&2; exit 1 ;;
        esac
        mv "$file.new" "$file"
        if [ "$kind" = httproute ]; then
            jq -r --arg s "$STABLE" --arg c "$CANARY" '[.spec.rules[].backendRefs // [] | select((map(.name) | index($s)) and (map(.name) | index($c)))]
                | first // [] | map({ (.name): .weight }) | add // {} | "\(.[$s]) \(.[$c])"' "$file" >> "$WEIGHTS_LOG"
        fi
        echo "$kind.gateway.networking.k8s.io/$name patched"
        ;;
    scale)
        [[ $replicas =~ ^[0-9]+$ ]] || { echo "fake kubectl: scale needs --replicas=N" >&2; exit 1; }
        jq --argjson r "$replicas" '.spec.replicas = $r' "$file" > "$file.new" && mv "$file.new" "$file"
        echo "deployment.apps/$name scaled"
        ;;
    "set image")
        [[ $image == *=* ]] || { echo "fake kubectl: set image needs CONTAINER=IMAGE" >&2; exit 1; }
        jq --arg c "${image%%=*}" --arg i "${image#*=}" '.spec.template.spec.containers |= ((. // []) | map(select(.name != $c)) + [{name: $c, image: $i}])' \
            "$file" > "$file.new" && mv "$file.new" "$file"
        echo "deployment.apps/$name image updated"
        ;;
    *) echo "fake kubectl: unsupported command $verb" >&2; exit 1 ;;
esac
KUBECTL

cat > "$fakebin/promq" <<'PROMQ'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$PROMQ_LOG"
n=$(wc -l < "$PROMQ_LOG")
answer=$(sed -n "${n}p" "$PROMQ_ANSWERS")
[ -n "$answer" ] || answer=$(tail -n 1 "$PROMQ_ANSWERS")
case "$answer" in
    error) echo "promq: the query failed" >&2; exit 1 ;;
    empty) exit 0 ;;
    *) echo "$answer" ;;
esac
PROMQ

cat > "$fakebin/sleep" <<'SLEEP'
#!/usr/bin/env bash
echo "$*" >> "$SLEEP_LOG"
n=$(wc -l < "$SLEEP_LOG")
if [ -n "${SLEEP_HOLD_AT:-}" ] && [ "$n" -eq "$SLEEP_HOLD_AT" ]; then
    : > "$SLEEP_HELD"
    while [ -e "$SLEEP_GATE" ]; do
        /usr/bin/sleep 0.05
    done
fi
exit 0
SLEEP
chmod +x "$fakebin/kubectl" "$fakebin/promq" "$fakebin/sleep"
export PATH="$fakebin:$PATH"

# The cluster: the HTTPRoute of k8s/route.yaml (a header rule for testers, then the
# weighted rule) and the canary Deployment, scaled to REPLICAS (default 0).
cluster() {
    jq -n --arg s "$STABLE" --arg c "$CANARY" '{
        apiVersion: "gateway.networking.k8s.io/v1", kind: "HTTPRoute",
        metadata: { name: "checkout", namespace: "shop" },
        spec: { parentRefs: [{ name: "public", namespace: "infra" }], hostnames: ["shop.example.com"],
          rules: [
            { matches: [{ headers: [{ type: "Exact", name: "x-canary", value: "always" }] }],
              backendRefs: [{ name: $c, port: 80 }] },
            { matches: [{ path: { type: "PathPrefix", value: "/" } }],
              backendRefs: [{ name: $s, port: 80, weight: 100 }, { name: $c, port: 80, weight: 0 }] } ] } }' \
        > "$KUBE_STATE/httproute-checkout.json"
    jq -n --argjson r "${1:-0}" --arg c "$CANARY" '{ apiVersion: "apps/v1", kind: "Deployment",
        metadata: { name: $c, namespace: "shop" }, spec: { replicas: $r } }' > "$KUBE_STATE/deployment-$CANARY.json"
}

# weights: the weights after each patch, one "STABLE CANARY" line each.
weights() {
    cat "$WEIGHTS_LOG"
}

# replicas: the canary Deployment's replicas now.
replicas() {
    jq -r '.spec.replicas' "$KUBE_STATE/deployment-$CANARY.json"
}

# answers VALUE...: what promq answers, call after call.
answers() {
    printf '%s\n' "$@" > "$PROMQ_ANSWERS"
}
