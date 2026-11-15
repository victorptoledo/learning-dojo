# Image that checks Kubernetes katas offline, with no cluster: the learner's manifests are
# rendered (plain YAML, `kustomize build` or `helm template`), validated with kubeconform
# against the Kubernetes 1.36 schemas baked in below, and checked by bats tests and jq
# policies over the rendered objects. Prints results in the `cargo test` format the dojo
# already reads (see tap2cargo.awk).

# ---- fetch: downloads pinned tools and schemas and verifies their checksums ----------
FROM debian:trixie-slim AS fetch

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

ARG KUBECONFORM_VERSION=0.8.0
ARG KUBECONFORM_SHA256=9bc2bffbf71f261128533edaf912153948b7ff238f9a531ae6d34466ec287883
ARG KUSTOMIZE_VERSION=5.8.3
ARG KUSTOMIZE_SHA256=cb9e31198d3f63b44848bd0afc4c8efc56e6cfef9c93a4a39a211940e6decd61
ARG HELM_VERSION=4.3.1
ARG HELM_SHA256=0403a88ae20913d11b10a2467b50087721fea7581736da6b9d3992f5357fe54b
ARG YQ_VERSION=4.54.1
ARG YQ_SHA256=8e34fc298390875de416e6a4afcb8cabeceb25d9aa8506c1a2f9353cf702ea5f

WORKDIR /fetch
RUN set -eu; \
    curl -fsSLo kubeconform.tgz "https://github.com/yannh/kubeconform/releases/download/v${KUBECONFORM_VERSION}/kubeconform-linux-amd64.tar.gz"; \
    echo "${KUBECONFORM_SHA256}  kubeconform.tgz" | sha256sum -c -; \
    curl -fsSLo kustomize.tgz "https://github.com/kubernetes-sigs/kustomize/releases/download/kustomize/v${KUSTOMIZE_VERSION}/kustomize_v${KUSTOMIZE_VERSION}_linux_amd64.tar.gz"; \
    echo "${KUSTOMIZE_SHA256}  kustomize.tgz" | sha256sum -c -; \
    curl -fsSLo helm.tgz "https://get.helm.sh/helm-v${HELM_VERSION}-linux-amd64.tar.gz"; \
    echo "${HELM_SHA256}  helm.tgz" | sha256sum -c -; \
    curl -fsSLo yq "https://github.com/mikefarah/yq/releases/download/v${YQ_VERSION}/yq_linux_amd64"; \
    echo "${YQ_SHA256}  yq" | sha256sum -c -; \
    mkdir bin; \
    tar -xzf kubeconform.tgz -C bin kubeconform; \
    tar -xzf kustomize.tgz -C bin kustomize; \
    tar -xzf helm.tgz --strip-components=1 -C bin linux-amd64/helm; \
    install -m 755 yq bin/yq

# The schemas of the built-in kinds the track uses, for one pinned Kubernetes release, in
# their strict form (unknown fields are errors), plus the Gateway API CRD schemas.
ARG KUBERNETES_VERSION=1.36.6
ARG K8S_SCHEMAS_COMMIT=2bfaff3898a7a4fa4e4760da3d2656211c9b4c71
ARG CRD_SCHEMAS_COMMIT=89c736bdde38b5fecd0f8436ec5f3aa3679a3b86
COPY kinds.txt crds.txt ./
RUN set -eu; \
    base="https://raw.githubusercontent.com/yannh/kubernetes-json-schema/${K8S_SCHEMAS_COMMIT}/v${KUBERNETES_VERSION}-standalone-strict"; \
    mkdir -p schemas/k8s; \
    grep -v '^#' kinds.txt | while read -r kind; do \
        [ -n "$kind" ] || continue; \
        curl -fsSLo "schemas/k8s/${kind}.json" "${base}/${kind}.json"; \
    done; \
    grep -v '^#' crds.txt | while read -r crd; do \
        [ -n "$crd" ] || continue; \
        mkdir -p "schemas/crds/$(dirname "$crd")"; \
        curl -fsSLo "schemas/crds/${crd}.json" "https://raw.githubusercontent.com/datreeio/CRDs-catalog/${CRD_SCHEMAS_COMMIT}/${crd}.json"; \
    done; \
    echo "${KUBERNETES_VERSION}" > schemas/kubernetes-version

# ---- the checker ---------------------------------------------------------------------
FROM debian:trixie-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bats bats-assert bats-support jq gawk sed grep findutils coreutils \
    && rm -rf /var/lib/apt/lists/*

COPY --from=fetch /fetch/bin/ /usr/local/bin/
COPY --from=fetch /fetch/schemas/ /opt/dojo/schemas/
COPY check.sh k8s.bash policies.jq tap2cargo.awk /opt/dojo/
RUN chmod 755 /opt/dojo/check.sh \
    && useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

ENV BATS_LIB_PATH=/usr/lib/bats \
    HELM_CACHE_HOME=/tmp/helm/cache \
    HELM_CONFIG_HOME=/tmp/helm/config \
    HELM_DATA_HOME=/tmp/helm/data \
    LANG=C.UTF-8 \
    TZ=UTC

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
