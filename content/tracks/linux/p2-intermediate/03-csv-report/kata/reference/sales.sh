#!/usr/bin/env bash
# Usage: sales.sh [-m YYYY-MM] [-r REGION] FILE - totals a sales CSV per region.
set -euo pipefail

usage() {
    echo "usage: sales.sh [-m YYYY-MM] [-r REGION] FILE" >&2
    exit 2
}

month=""
region=""
while getopts ':m:r:' opt; do
    case "$opt" in
        m) month="$OPTARG" ;;
        r) region="$OPTARG" ;;
        *) usage ;;
    esac
done
shift $((OPTIND - 1))
[ "$#" -eq 1 ] || usage

if [[ -n "$month" && ! "$month" =~ ^[0-9]{4}-(0[1-9]|1[0-2])$ ]]; then
    echo "sales.sh: invalid month: $month" >&2
    exit 2
fi
file="$1"
if [ ! -r "$file" ]; then
    echo "sales.sh: cannot read $file" >&2
    exit 1
fi

echo "region,orders,units,revenue"
awk -F, -v month="$month" -v region="$region" '
    NR == 1 { next }
    { sub(/\r$/, "") }
    /^[[:space:]]*$/ { next }
    NF != 5 || $4 !~ /^[1-9][0-9]*$/ || $5 !~ /^[0-9]+(\.[0-9][0-9]?)?$/ {
        printf "sales.sh: line %d: invalid record\n", NR > "/dev/stderr"
        next
    }
    month != "" && substr($1, 1, 7) != month { next }
    region != "" && $2 != region { next }
    {
        orders[$2]++
        units[$2] += $4
        revenue[$2] += $4 * $5
        total_orders++
        total_units += $4
        total_revenue += $4 * $5
    }
    END {
        sorter = "LC_ALL=C sort"
        for (r in orders) printf "%s,%d,%d,%.2f\n", r, orders[r], units[r], revenue[r] | sorter
        close(sorter)
        printf "TOTAL,%d,%d,%.2f\n", total_orders, total_units, total_revenue
    }
' "$file"
