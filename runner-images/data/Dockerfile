# Image that checks the Data Engineer track's Parquet and DuckDB katas, offline.
# It is dojo-python plus pyarrow and DuckDB: the same check.sh and junit2cargo.py (a syntax
# check, the pytest tests and ruff), printing results in the `cargo test` format.
# The Python katas that need only the standard library stay on dojo-python.
FROM python:3.15-slim-trixie

RUN pip install --no-cache-dir --disable-pip-version-check --root-user-action=ignore \
        pytest==9.2.0 ruff==0.17.2 pyarrow==26.0.1 duckdb==2.0.0

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
