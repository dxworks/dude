#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Usage: prepare-release-voyager.sh <version>}"

echo "$VERSION" > src/main/resources/dude-version

mkdir -p dude/results dude/logs
cp README.md dude/README.md
cp target/dude.jar dude/dude.jar
cp instrument.yml dude/instrument.yml
cp languages.yml dude/languages.yml
cp .ignore dude/.ignore

zip -r dude-voyager.zip dude
