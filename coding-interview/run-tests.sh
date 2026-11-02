#!/usr/bin/env bash
# Runs the coding-interview tests (playbook-examples, package codinginterview) and writes one report per class to outputs/.
#   coding-interview/run-tests.sh          run tests, (re)write outputs/
#   coding-interview/run-tests.sh --check  fail if any test fails or a report differs from the committed one
# Extra Gradle arguments can be passed via EXTRA_GRADLE_ARGS (for example an init script).
set -uo pipefail
cd "$(dirname "$0")/.."
# shellcheck disable=SC2086
./gradlew :playbook-examples:test --tests '*codinginterview*' --no-daemon -q ${EXTRA_GRADLE_ARGS:-}
gradle_rc=$?
cd coding-interview
python3 report.py "${1:-write}"
report_rc=$?
exit $(( gradle_rc != 0 || report_rc != 0 ))
