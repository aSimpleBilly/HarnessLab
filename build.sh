#!/bin/sh
set -eu

case "${1:-build}" in
    build|test) ;;
    *) printf 'Usage: sh build.sh [build|test]\n' >&2; exit 2 ;;
esac

cd "$(dirname "$0")"
mkdir -p build/classes
javac --release 21 -d build/classes src/main/java/lab/HarnessLabHub.java
jar --create --file build/harness-lab-hub.jar --main-class lab.HarnessLabHub \
    -C build/classes . -C src/main/resources .

if [ "${1:-build}" = test ]; then
    mkdir -p build/test-classes
    javac --release 21 -cp build/harness-lab-hub.jar -d build/test-classes \
        src/test/java/lab/HarnessLabHubUnitTest.java src/test/java/lab/HarnessLabHubTest.java
    java -cp build/harness-lab-hub.jar:build/test-classes lab.HarnessLabHubUnitTest
    java -cp build/harness-lab-hub.jar:build/test-classes lab.HarnessLabHubTest
fi
