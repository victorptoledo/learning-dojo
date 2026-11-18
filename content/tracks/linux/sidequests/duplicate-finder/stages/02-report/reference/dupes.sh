#!/usr/bin/env bash
# Usage: dupes.sh scan|report|link [options] DIR - finds files with the same content.
set -uo pipefail
export LC_ALL=C

usage() {
    echo "usage: dupes.sh scan|report|link [options] DIR" >&2
    exit 2
}

# N, NK or NM -> bytes.
parse_size() {
    case $1 in
        *[!0-9KM]* | "" | [KM]* | *[KM]?*) echo "dupes.sh: bad size: $1" >&2; return 1 ;;
        *K) echo $(( ${1%K} * 1024 )) ;;
        *M) echo $(( ${1%M} * 1048576 )) ;;
        *) echo "$1" ;;
    esac
}

# SIZE<TAB>INODE<TAB>PATH for every non-empty regular file of at least MIN bytes whose size
# another such file shares. Files with a size of their own are never opened.
same_size_files() {
    find "$1" -type f -size +0 -printf '%s\t%i\t%P\n' |
        awk -F'\t' -v min="$2" '
            $1 >= min { count[$1]++; line[++n] = $0; size[n] = $1 }
            END { for (i = 1; i <= n; i++) if (count[size[i]] > 1) print line[i] }'
}

# HASH<TAB>SIZE<TAB>INODE<TAB>PATH, sorted by hash and then path.
hashed() {
    local dir=$1 size inode path sum
    while IFS=$'\t' read -r size inode path; do
        sum=$(sha256sum < "$dir/$path") || continue
        printf '%s\t%s\t%s\t%s\n' "${sum%% *}" "$size" "$inode" "$path"
    done | sort -t $'\t' -k1,1 -k4
}

# FIRST<TAB>SIZE<TAB>INODE<TAB>PATH for each file of each group of two or more, where FIRST
# names the group (its first path); sorted by group and then path.
collect() {
    same_size_files "$1" "$2" | hashed "$1" |
        awk -F'\t' '
            function flush(   i) { if (n > 1) for (i = 1; i <= n; i++) print first "\t" rec[i]; n = 0 }
            $1 != hash { flush(); hash = $1; first = $4 }
            { rec[++n] = $2 "\t" $3 "\t" $4 }
            END { flush() }' |
        sort -t $'\t' -k1,1 -k4
}

print_scan() {
    awk -F'\t' 'NR > 1 && $1 != prev { print "" } { prev = $1; print $4 }'
}

# Wasted space of a group: its size once for every inode but one (hard links are one file).
print_report() {
    awk -F'\t' '
        function flush() {
            if (n > 0 && inodes > 1)
                printf "%d\t%s\t%d files of %d bytes, %d wasted:%s\n", size * (inodes - 1), first, n, size, size * (inodes - 1), paths
            n = 0; inodes = 0; paths = ""; split("", seen)
        }
        $1 != first { flush(); first = $1; size = $2 }
        { n++; paths = paths "\037" $4; if (!($3 in seen)) { seen[$3] = 1; inodes++ } }
        END { flush() }' |
        sort -t $'\t' -k1,1nr -k2,2 |
        awk -F'\t' '
            { count = split($3, part, "\037"); for (i = 1; i <= count; i++) print part[i]; print ""; total += $1; groups++ }
            END { printf "total: %d bytes wasted in %d %s\n", total, groups, groups == 1 ? "group" : "groups" }'
}

command=${1:-}
[ "$#" -gt 0 ] && shift
case $command in
    scan | report) ;;
    *) usage ;;
esac
min=1
while [ "$#" -gt 1 ]; do
    case $1 in
        --min-size)
            min=$(parse_size "$2") || exit 2
            shift 2
            ;;
        *) usage ;;
    esac
done
[ "$#" -eq 1 ] || usage
dir=$1
if [ ! -d "$dir" ]; then
    echo "dupes.sh: not a directory: $dir" >&2
    exit 2
fi

case $command in
    scan) collect "$dir" "$min" | print_scan ;;
    report) collect "$dir" "$min" | print_report ;;
esac
