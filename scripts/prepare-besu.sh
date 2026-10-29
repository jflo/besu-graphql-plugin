#!/usr/bin/env bash
#
# Copyright contributors to Besu.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
# specific language governing permissions and limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

# Makes the Besu version the plugin is built against (`besu` in gradle/libs.versions.toml)
# available, and prints the path of a Besu installation of that version on its last line, for
# `./gradlew integrationTest -PbesuHome=<path>`.
#
# - A released version: the build downloads Besu's Maven artifacts by itself; this script
#   downloads and unpacks the Besu distribution from the Besu GitHub release.
# - A -SNAPSHOT version (the plugin API the plugin needs is not released yet): this script fetches
#   Besu from besuSnapshotRepository at besuSnapshotCommit (see gradle.properties), publishes it to
#   the local Maven repository under that version and installs its distribution. Building Besu
#   needs a JDK 25 (JAVA_HOME, or java on the PATH).
#
# Usage: scripts/prepare-besu.sh [work directory, default: build/besu]

set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
work="${1:-$root/build/besu}"
version="$(sed -nE 's/^besu *= *"([^"]+)".*/\1/p' "$root/gradle/libs.versions.toml")"
property() { sed -nE "s/^$1=(.*)$/\1/p" "$root/gradle.properties"; }

if [ -z "$version" ]; then
  echo "No besu version in gradle/libs.versions.toml" >&2
  exit 1
fi
mkdir -p "$work"

if [[ "$version" == *-SNAPSHOT ]]; then
  repository="$(property besuSnapshotRepository)"
  commit="$(property besuSnapshotCommit)"
  if [[ ! "$commit" =~ ^[0-9a-f]{40}$ ]]; then
    echo "besuSnapshotCommit in gradle.properties must be a full commit hash, not '$commit'" >&2
    exit 1
  fi
  java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
  java_version="$("$java" -XshowSettings:properties -version 2>&1 \
    | sed -nE 's/^ *java\.specification\.version = ([0-9]+).*/\1/p')" || true
  if [ "${java_version:-0}" -lt 25 ]; then
    echo "Building Besu needs a JDK 25 (JAVA_HOME, or java on the PATH); found ${java_version:-none}" >&2
    exit 1
  fi
  source="$work/besu-src"
  if [ ! -d "$source/.git" ]; then
    git init --quiet "$source"
  fi
  # A clone from an earlier run is moved to the commit gradle.properties names now.
  if [ "$(git -C "$source" rev-parse --verify --quiet HEAD || true)" != "$commit" ]; then
    echo "Fetching Besu $commit from $repository" >&2
    git -C "$source" fetch --quiet --depth 1 "$repository" "$commit" >&2
    git -C "$source" checkout --quiet --force --detach FETCH_HEAD >&2
    git -C "$source" clean --quiet -fdx >&2
  fi
  if [ "$(git -C "$source" rev-parse HEAD)" != "$commit" ]; then
    echo "Besu sources in $source are not at $commit" >&2
    exit 1
  fi
  echo "Publishing Besu $version to the local Maven repository" >&2
  (cd "$source" && ./gradlew --no-daemon --quiet publishToMavenLocal installDist -Pversion="$version" >&2)
  echo "$source/build/install/besu"
else
  archive="besu-$version.tar.gz"
  if [ ! -d "$work/besu-$version" ]; then
    echo "Downloading Besu $version" >&2
    curl --fail --silent --show-error --location --output "$work/$archive" \
      "https://github.com/besu-eth/besu/releases/download/$version/$archive"
    tar -xzf "$work/$archive" -C "$work"
  fi
  echo "$work/besu-$version"
fi
