#!/usr/bin/env bash
# Usage: logstats.sh FILE - summarises an access log.
if [ "$#" -ne 1 ]; then
    echo "usage: logstats.sh FILE" >&2
    exit 2
fi
log="$1"
if [ ! -r "$log" ]; then
    echo "logstats.sh: cannot read $log" >&2
    exit 1
fi

echo "requests: $(wc -l < "$log")"
echo "visitors: $(cut -d' ' -f1 "$log" | sort -u | wc -l)"
echo "errors: $(cut -d' ' -f9 "$log" | grep -c '^[45]' || true)"
echo "top paths:"
cut -d' ' -f7 "$log" | sort | uniq -c | sort -k1,1nr -k2,2 | head -n 3 | sed 's/^ *//'
