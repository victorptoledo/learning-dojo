# Image that checks AWS katas offline, with no AWS account and no credentials:
# CloudFormation and SAM templates are linted by cfn-lint and checked by cfn-guard rules
# and pytest tests over the parsed template; IAM policies are evaluated by the dojo's own
# policy evaluator (dojo_aws/iam.py) against a matrix of requests; Python code that calls
# AWS (boto3) runs against moto, which simulates the services in memory.
# Prints results in the `cargo test` format the dojo already reads (see report.py).

# ---- fetch: downloads cfn-guard and verifies its checksum ----------------------------
FROM debian:trixie-slim AS fetch

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

ARG CFN_GUARD_VERSION=3.2.1
ARG CFN_GUARD_SHA256=8c66efb19c63e6c2bf26b9a41bbcf2f85baa8a937b01d350940194faaf64cf1d

WORKDIR /fetch
RUN set -eu; \
    curl -fsSLo guard.tgz "https://github.com/aws-cloudformation/cloudformation-guard/releases/download/${CFN_GUARD_VERSION}/cfn-guard-v3-x86_64-linux-latest.tar.gz"; \
    echo "${CFN_GUARD_SHA256}  guard.tgz" | sha256sum -c -; \
    mkdir bin; \
    tar -xzf guard.tgz --strip-components=1 -C bin cfn-guard-v3-x86_64-linux-latest/cfn-guard; \
    chmod 755 bin/cfn-guard

# ---- the checker ---------------------------------------------------------------------
FROM python:3.15-slim-trixie

RUN pip install --no-cache-dir --disable-pip-version-check --root-user-action=ignore \
        pytest==9.2.0 ruff==0.17.2 cfn-lint==1.60.1 boto3==1.44.3 \
        "moto[dynamodb,s3,sqs,sns,iam,events]==5.2.5"

COPY --from=fetch /fetch/bin/cfn-guard /usr/local/bin/cfn-guard
COPY check.sh report.py /opt/dojo/
COPY dojo_aws/ /opt/dojo/dojo_aws/
COPY selftest/ /tmp/selftest/
# The IAM evaluator's own tests: a build with broken policy semantics fails here.
RUN PYTHONPATH=/opt/dojo python -m pytest -q -p no:cacheprovider /tmp/selftest     && rm -rf /tmp/selftest
RUN chmod 755 /opt/dojo/check.sh \
    && useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

# moto needs credentials to sign requests; these are fake and never leave the container,
# which runs with no network.
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/opt/dojo \
    AWS_ACCESS_KEY_ID=testing \
    AWS_SECRET_ACCESS_KEY=testing \
    AWS_SESSION_TOKEN=testing \
    AWS_DEFAULT_REGION=eu-west-1 \
    AWS_EC2_METADATA_DISABLED=true \
    LANG=C.UTF-8 \
    TZ=UTC

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
