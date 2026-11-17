# Image that checks Python katas: a syntax check, the pytest tests and ruff, offline.
# Prints results in the `cargo test` format the dojo already reads (see junit2cargo.py).
# FastAPI and httpx (for its TestClient) are there for the Python task API sidequest.
FROM python:3.15-slim-trixie

RUN pip install --no-cache-dir --disable-pip-version-check --root-user-action=ignore \
        pytest==9.2.0 ruff==0.17.2 \
        fastapi==0.162.0 starlette==1.9.0 pydantic==2.14.1 httpx==0.28.1

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
