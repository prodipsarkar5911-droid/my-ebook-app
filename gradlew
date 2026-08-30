#!/usr/bin/env bash
# Direct Gradle execution wrapper for Linux/GitHub Actions
set -e
export org.gradle.jvmargs="-Xmx2048m -Dfile.encoding=UTF-8"
exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
