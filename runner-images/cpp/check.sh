#!/bin/bash
# Checks a C++ kata in /work: the code must compile with GCC 16 in C++23 mode, the visible and
# grading tests (staged in /work/tests) run under the sanitizers, and the compiler warnings,
# clang-format and clang-tidy check the learner's files (the .cpp/.hpp/.h files at the top of
# the kata folder). Prints the result in `cargo test` format (see report/dojo_report.cpp).
#
# Sanitizers: address + undefined behaviour (with leak detection) by default; thread when the
# grading tests hold a file `sanitize` that says `thread`.
set -u
shopt -s nullglob

out=/target
export CCACHE_DIR="$out/ccache" CCACHE_MAXSIZE=400M CCACHE_NOSTATS=1
# The compiler cache survives between checks of the same kata; everything else starts clean.
find "$out" -mindepth 1 -maxdepth 1 ! -name ccache -exec rm -rf {} +
mkdir -p "$out/obj" "$out/logs" "$out/extra"

# Everything runs on a copy, so code that writes or deletes files cannot harm /work.
cp -r /work "$out/kata"
cd "$out/kata" || exit 1

sources=(*.cpp)
learner=(*.cpp *.hpp *.h)
tests=(tests/*.cpp)
if [ ${#learner[@]} -eq 0 ]; then
    echo "error: no .cpp or .hpp files at the top of the kata folder"
    exit 1
fi
if [ ${#tests[@]} -eq 0 ]; then
    echo "error: no tests in tests/"
    exit 1
fi

flavor=asan
sanitize=(-fsanitize=address,undefined -fno-sanitize-recover=all)
if grep -qs thread tests/sanitize; then
    flavor=tsan
    sanitize=(-fsanitize=thread)
fi
gtest=/opt/dojo/gtest-$flavor
flags=(-std=c++23 -g -O1 -fno-omit-frame-pointer -D_GLIBCXX_ASSERTIONS -pthread
    -fdiagnostics-color=never "${sanitize[@]}" -I.)
warnings=(-Wall -Wextra -Wpedantic -Wshadow -Wnon-virtual-dtor -Woverloaded-virtual)

# The compiler output with the sandbox paths removed.
tidy_log() { sed -e "s#$out/kata/##g" -e 's#^\./##' "$@"; }

# Compiles each file in the background (two at a time, the sandbox has two CPUs).
compile_all() {
    local file status=0
    local -a pids=()
    for file in "$@"; do
        local object="$out/obj/${file//\//_}.o"
        ccache g++ "${flags[@]}" "${warnings[@]}" -isystem "$gtest/include" \
            -c "$file" -o "$object" > "$out/logs/${file//\//_}.log" 2>&1 &
        pids+=($!)
        if [ ${#pids[@]} -ge 2 ]; then
            wait "${pids[0]}" || status=1
            pids=("${pids[@]:1}")
        fi
    done
    for pid in "${pids[@]}"; do
        wait "$pid" || status=1
    done
    return $status
}

# An error in a learner file is the learner's; anything else is a test that no longer fits.
learner_error() {
    tidy_log "$out"/logs/*.log | grep -Eq '^[A-Za-z0-9_.-]+\.(cpp|hpp|h):[0-9]+(:[0-9]+)?: (fatal )?error'
}

if ! compile_all "${sources[@]}" "${tests[@]}"; then
    if learner_error || ! tidy_log "$out"/logs/*.log | grep -q '^tests/.*error'; then
        echo "error: the code does not compile"
    else
        echo "error: the tests do not compile (did a function, type or header get renamed?)"
    fi
    tidy_log "$out"/logs/*.log | grep -v ': warning: \|^In file included from\|^  *from ' |
        grep -v '^cc1plus: ' | head -60
    exit 1
fi

if ! g++ "${flags[@]}" "$out"/obj/*.o -L"$gtest/lib" -ldojo_main -lgmock -lgtest \
    -o "$out/kata-tests" > "$out/link.log" 2>&1; then
    echo "error: the code does not link (is a function declared but never defined?)"
    tidy_log "$out/link.log" | grep -v '^collect2' | sed 's#/usr/bin/ld: ##' | head -30
    exit 1
fi

# One extra result: name, then ok or FAILED, then the message.
result() {
    local file="$out/extra/$1" name="$2" message="$3"
    if [ -z "$message" ]; then
        printf '%s\nok\n' "$name" > "$file"
    else
        printf '%s\nFAILED\n%s\n' "$name" "$message" > "$file"
    fi
}

# Warnings in the learner's files, from every compile (headers show up in the test compiles).
found=$(tidy_log "$out"/logs/*.log |
    grep -E '^[A-Za-z0-9_.-]+\.(cpp|hpp|h):[0-9]+(:[0-9]+)?: warning' | sort -u | head -8)
result 10-gcc gcc_warnings "${found:+$found
Fix them: g++ -Wall -Wextra -Wpedantic -Wshadow flags each one as a likely bug.}"

style=/opt/dojo/clang-format
for file in "${learner[@]}"; do
    name=${file//./_}
    if ! clang-format --style="file:$style" --dry-run -Werror "$file" > /dev/null 2>&1; then
        line=$(clang-format --style="file:$style" --dry-run "$file" 2>&1 | grep -m1 -oE ':[0-9]+:' | tr -d ':')
        result "20-format-$name" "clang_format::$name" "$file is not formatted (first difference near line ${line:-1}): run clang-format, or format on save in VS Code with the kata's .clang-format"
    else
        result "20-format-$name" "clang_format::$name" ""
    fi
done

# clang-tidy reads GCC 16's own headers, so it sees the same standard library as the compiler.
gcc_dir=$(dirname "$(g++ -print-libgcc-file-name)")
tidy_one() {
    local file="$1" name=${1//./_}
    local found
    found=$(clang-tidy --quiet --config-file=/opt/dojo/clang-tidy "$file" -- \
        -std=c++23 -I. -D_GLIBCXX_ASSERTIONS --gcc-install-dir="$gcc_dir" 2>/dev/null |
        tidy_log | grep -E '^[A-Za-z0-9_.-]+:[0-9]+:[0-9]+: (warning|error)' | head -6)
    result "30-tidy-$name" "clang_tidy::$name" "$found"
}
pids=()
for file in "${learner[@]}"; do
    tidy_one "$file" &
    pids+=($!)
    if [ ${#pids[@]} -ge 2 ]; then
        wait "${pids[0]}"
        pids=("${pids[@]:1}")
    fi
done
wait

export ASAN_OPTIONS=detect_leaks=1:abort_on_error=0:print_summary=1
export UBSAN_OPTIONS=print_stacktrace=1
export TSAN_OPTIONS=halt_on_error=1:second_deadlock_stack=1
if [ "$flavor" = asan ]; then
    export DOJO_LEAK_CHECK=1
fi
exec /opt/dojo/dojo-report "$out/kata-tests" "$out/extra"
