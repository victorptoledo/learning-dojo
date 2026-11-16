# Image that checks DevOps katas offline: GitHub Actions workflows (actionlint, with
# ShellCheck for `run:` scripts), Terraform modules (fmt, validate and `terraform test`, with
# the providers baked in as a filesystem mirror), Prometheus rules (promtool check and test)
# and bats tests over all of them. Prints results in the `cargo test` format the dojo
# already reads (see tap2cargo.awk).

# ---- fetch: downloads pinned tools and verifies their checksums ----------------------
FROM debian:trixie-slim AS fetch

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl unzip \
    && rm -rf /var/lib/apt/lists/*

ARG ACTIONLINT_VERSION=1.7.12
ARG ACTIONLINT_SHA256=8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8
ARG TERRAFORM_VERSION=1.17.2
ARG TERRAFORM_SHA256=e6f16b2f0b137ee5729caa8c4e79a3bae4ee261acd198a1fcfa8584a6296605b
ARG PROMETHEUS_VERSION=3.16.0
ARG PROMETHEUS_SHA256=68727009472a2c02e1ff7f062fd5b3e637285bffc8fc2d340c76fb449d6e0f34
ARG YQ_VERSION=4.54.1
ARG YQ_SHA256=8e34fc298390875de416e6a4afcb8cabeceb25d9aa8506c1a2f9353cf702ea5f

WORKDIR /fetch
RUN set -eu; \
    curl -fsSLo actionlint.tgz "https://github.com/rhysd/actionlint/releases/download/v${ACTIONLINT_VERSION}/actionlint_${ACTIONLINT_VERSION}_linux_amd64.tar.gz"; \
    echo "${ACTIONLINT_SHA256}  actionlint.tgz" | sha256sum -c -; \
    curl -fsSLo terraform.zip "https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip"; \
    echo "${TERRAFORM_SHA256}  terraform.zip" | sha256sum -c -; \
    curl -fsSLo prometheus.tgz "https://github.com/prometheus/prometheus/releases/download/v${PROMETHEUS_VERSION}/prometheus-${PROMETHEUS_VERSION}.linux-amd64.tar.gz"; \
    echo "${PROMETHEUS_SHA256}  prometheus.tgz" | sha256sum -c -; \
    curl -fsSLo yq "https://github.com/mikefarah/yq/releases/download/v${YQ_VERSION}/yq_linux_amd64"; \
    echo "${YQ_SHA256}  yq" | sha256sum -c -; \
    mkdir bin; \
    tar -xzf actionlint.tgz -C bin actionlint; \
    unzip -q terraform.zip terraform -d bin; \
    tar -xzf prometheus.tgz --strip-components=1 -C bin "prometheus-${PROMETHEUS_VERSION}.linux-amd64/promtool"; \
    install -m 755 yq bin/yq

# The providers the katas may use, as an unpacked filesystem mirror, so `terraform init`
# works with no network. `name version sha256` per line.
COPY providers.txt ./
RUN set -eu; \
    grep -v '^#' providers.txt | while read -r name version sum; do \
        [ -n "$name" ] || continue; \
        file="terraform-provider-${name}_${version}_linux_amd64.zip"; \
        curl -fsSLo "$file" "https://releases.hashicorp.com/terraform-provider-${name}/${version}/${file}"; \
        echo "${sum}  ${file}" | sha256sum -c -; \
        dir="mirror/registry.terraform.io/hashicorp/${name}/${version}/linux_amd64"; \
        mkdir -p "$dir"; \
        unzip -q "$file" -d "$dir"; \
    done

# ---- the checker ---------------------------------------------------------------------
FROM debian:trixie-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bats bats-assert bats-support jq shellcheck gawk sed grep findutils coreutils \
    && rm -rf /var/lib/apt/lists/*

COPY --from=fetch /fetch/bin/ /usr/local/bin/
COPY --from=fetch /fetch/mirror/ /opt/dojo/terraform-providers/
COPY check.sh devops.bash policies.jq tap2cargo.awk terraformrc /opt/dojo/
RUN chmod 755 /opt/dojo/check.sh \
    && useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

ENV BATS_LIB_PATH=/usr/lib/bats \
    TF_CLI_CONFIG_FILE=/opt/dojo/terraformrc \
    TF_IN_AUTOMATION=1 \
    TF_INPUT=0 \
    CHECKPOINT_DISABLE=1 \
    LANG=C.UTF-8 \
    TZ=UTC

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
