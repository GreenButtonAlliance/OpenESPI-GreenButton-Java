#!/usr/bin/env bash
# Resolve release metadata for a per-module release tag (see RELEASING.md).
#
#   usage: release-meta.sh <tag>            e.g. datacustodian-v4.0.0
#
# Validates the tag, maps the module id to its directory / artifactId, checks that the
# module's pom version at the tagged commit is <X.Y.Z>-SNAPSHOT, finds the previous tag of the
# same module and computes the next development version (<X.Y.Z+1>-SNAPSHOT).
#
# Prints key=value lines (append them to "$GITHUB_OUTPUT" in CI). Exits non-zero on any error.
#
# POM_VERSION may be set to skip the Maven lookup (used for local dry runs / tests).
set -euo pipefail

tag="${1:?usage: release-meta.sh <module>-v<X.Y.Z>}"

if [[ ! "$tag" =~ ^([a-z]+)-v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]]; then
  echo "::error::Tag '$tag' is not of the form <module>-v<X.Y.Z> (e.g. datacustodian-v4.0.0)" >&2
  exit 1
fi
module="${BASH_REMATCH[1]}"
major="${BASH_REMATCH[2]}"
minor="${BASH_REMATCH[3]}"
patch="${BASH_REMATCH[4]}"
version="${major}.${minor}.${patch}"

# module id -> directory, artifactId, library/application, parent property holding its version,
# and the in-repo modules it depends on (upstream, comma-separated, for `mvn -pl`).
case "$module" in
  handoff)       dir=openespi-handoff;       artifact=OpenESPI-Handoff;             kind=library;     version_property=openespi-handoff.version; upstream= ;;
  common)        dir=openespi-common;        artifact=OpenESPI-Common;              kind=library;     version_property=openespi-common.version;  upstream=openespi-handoff ;;
  datacustodian) dir=openespi-datacustodian; artifact=OpenESPI-DataCustodian;       kind=application; version_property=;                         upstream=openespi-handoff,openespi-common ;;
  thirdparty)    dir=openespi-thirdparty;    artifact=OpenESPI-ThirdParty;          kind=application; version_property=;                         upstream=openespi-handoff,openespi-common ;;
  authserver)    dir=openespi-authserver;    artifact=OpenESPI-AuthorizationServer; kind=application; version_property=;                         upstream=openespi-handoff ;;
  *)
    echo "::error::Unknown module '$module' in tag '$tag'. Expected one of: handoff, common, datacustodian, thirdparty, authserver" >&2
    exit 1
    ;;
esac

if [[ -z "${POM_VERSION:-}" ]]; then
  POM_VERSION="$(mvn -B -q -N -f "${dir}/pom.xml" help:evaluate -Dexpression=project.version -DforceStdout)"
fi
if [[ "$POM_VERSION" != "${version}-SNAPSHOT" ]]; then
  echo "::error::${dir}/pom.xml version at the tagged commit is '${POM_VERSION}', expected '${version}-SNAPSHOT' for tag '${tag}'" >&2
  exit 1
fi

next_version="${major}.${minor}.$((patch + 1))-SNAPSHOT"

# Extra Maven test arguments per module.
test_args=""
if [[ "$module" == authserver ]]; then
  # TODO(#129): AS unit tests are broken on the H2 test profiles; only the testcontainers-it suite runs.
  test_args="-Dgroups=testcontainers-it -DfailIfNoTests=false"
fi

# Previous release tag of the SAME module (highest version below this one), for release notes.
previous_tag=""
while IFS= read -r t; do
  [[ -z "$t" || "$t" == "$tag" ]] && continue
  # first entry (sorted descending) that sorts below the current tag
  if [[ "$(printf '%s\n%s\n' "$t" "$tag" | sort -V | head -n1)" == "$t" ]]; then
    previous_tag="$t"
    break
  fi
done < <(git tag --list "${module}-v[0-9]*.[0-9]*.[0-9]*" --sort=-v:refname)

cat <<EOF
module=${module}
dir=${dir}
artifact=${artifact}
kind=${kind}
version=${version}
next_version=${next_version}
version_property=${version_property}
upstream=${upstream}
previous_tag=${previous_tag}
test_args=${test_args}
EOF
