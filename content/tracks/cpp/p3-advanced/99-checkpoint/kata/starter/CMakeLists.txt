# Builds this kata and its visible tests on your machine (optional: the dojo checks it in
# Docker). From this folder: cmake -B build && cmake --build build && ctest --test-dir build
cmake_minimum_required(VERSION 3.28)
project(kata LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# Uses an installed GoogleTest, or downloads it once into build/.
find_package(GTest QUIET)
if(NOT GTest_FOUND)
  include(FetchContent)
  FetchContent_Declare(googletest
    URL https://github.com/google/googletest/archive/refs/tags/v1.17.0.tar.gz)
  set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
  FetchContent_MakeAvailable(googletest)
endif()

file(GLOB KATA_SOURCES CONFIGURE_DEPENDS *.cpp)
file(GLOB KATA_TESTS CONFIGURE_DEPENDS tests/*.cpp)
add_executable(kata_tests ${KATA_SOURCES} ${KATA_TESTS})
target_include_directories(kata_tests PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
find_package(Threads REQUIRED)
target_link_libraries(kata_tests PRIVATE GTest::gtest_main Threads::Threads)
if(NOT MSVC)
  target_compile_options(kata_tests PRIVATE -Wall -Wextra -Wpedantic -Wshadow)
endif()

include(GoogleTest)
gtest_discover_tests(kata_tests)
