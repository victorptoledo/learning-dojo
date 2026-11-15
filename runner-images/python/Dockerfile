# Image that checks Python katas: a syntax check, the pytest tests and ruff, offline.
# Prints results in the `cargo test` format the dojo already reads (see junit2cargo.py).
FROM python:3.15-slim-trixie

RUN pip install --no-cache-dir --disable-pip-version-check --root-user-action=ignore \
        pytest==9.2.0 ruff==0.17.2

COPY check.sh junit2cargo.py /opt/dojo/
RUN chmod 755 /opt/dojo/check.sh \
    && useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    LANG=C.UTF-8 \
    TZ=UTC

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
