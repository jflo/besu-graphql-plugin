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
# - A -SNAPSHOT version (the plugin API the plugin needs is not released yet): this script clones
#   Besu from besuSnapshotRepository at besuSnapshotRef (see gradle.properties), publishes it to
#   the local Maven repository under that version and installs its distribution.
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
  ref="$(property besuSnapshotRef)"
  source="$work/besu-src"
  if [ ! -d "$source/.git" ]; then
    echo "Cloning Besu $ref from $repository" >&2
    git clone --quiet --depth 1 --branch "$ref" "$repository" "$source" >&2
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
