# Image that checks C++ katas: GCC 16 in C++23 mode, GoogleTest, the sanitizers (address and
# undefined behaviour by default, thread for concurrency katas), clang-tidy and clang-format,
# offline. Prints results in the `cargo test` format the dojo already reads (see report/).
FROM gcc:16

# Installed once at build time: checks run with --network none. googletest is the Debian
# source package; it is compiled below with GCC 16 and the same sanitizer flags as the katas.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ccache clang-format clang-tidy cmake googletest ninja-build \
    && rm -rf /var/lib/apt/lists/*

ENV LANG=C.UTF-8 \
    TZ=UTC

COPY report /opt/dojo/report
COPY check.sh clang-format clang-tidy /opt/dojo/

# One GoogleTest build and one test main per sanitizer: `asan` (address + undefined) and `tsan`.
RUN set -eu; \
    for flavor in asan tsan; do \
        if [ "$flavor" = asan ]; then sanitize="-fsanitize=address,undefined -fno-sanitize-recover=all"; \
        else sanitize="-fsanitize=thread"; fi; \
        flags="-g -O1 -fno-omit-frame-pointer -D_GLIBCXX_ASSERTIONS $sanitize"; \
        cmake -S /usr/src/googletest -B /tmp/gtest-$flavor -G Ninja \
            -DCMAKE_BUILD_TYPE=None -DCMAKE_CXX_STANDARD=23 -DCMAKE_CXX_FLAGS="$flags" \
            -DCMAKE_INSTALL_PREFIX=/opt/dojo/gtest-$flavor > /dev/null; \
        cmake --build /tmp/gtest-$flavor > /dev/null; \
        cmake --install /tmp/gtest-$flavor > /dev/null; \
        g++ -std=c++23 $flags -I/opt/dojo/gtest-$flavor/include \
            -c /opt/dojo/report/dojo_main.cpp -o /tmp/dojo_main.o; \
        ar rcs /opt/dojo/gtest-$flavor/lib/libdojo_main.a /tmp/dojo_main.o; \
    done; \
    rm -rf /tmp/gtest-* /tmp/dojo_main.o; \
    g++ -std=c++23 -O2 -Wall -Wextra -Werror -o /opt/dojo/dojo-report /opt/dojo/report/dojo_report.cpp; \
    chmod 755 /opt/dojo/check.sh; \
    useradd --create-home --uid 10001 dojo; \
    mkdir /target; \
    chown dojo /target

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
