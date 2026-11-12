#!/usr/bin/env bash
# Set ONE module's version (and, for a library, the parent property consumers use).
#
#   usage: set-module-version.sh <module-dir> <new-version> [<parent-version-property>]
#
# Used by release.yml both for the CI-only release version (never committed) and for the
# post-release bump to the next -SNAPSHOT (committed via a PR).
set -euo pipefail

dir="${1:?module dir}"
new_version="${2:?new version}"
property="${3:-}"

mvn -q versions:set -pl "$dir" -DnewVersion="$new_version" -DgenerateBackupPoms=false
if [[ -n "$property" ]]; then
  mvn -q -N versions:set-property -Dproperty="$property" -DnewVersion="$new_version" -DgenerateBackupPoms=false
fi

actual="$(mvn -q -N -f "${dir}/pom.xml" help:evaluate -Dexpression=project.version -DforceStdout)"
if [[ "$actual" != "$new_version" ]]; then
  echo "::error::${dir} version is '${actual}' after versions:set, expected '${new_version}'" >&2
  exit 1
fi
echo "${dir} -> ${new_version}${property:+ (parent property ${property})}"
