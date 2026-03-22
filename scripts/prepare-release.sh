#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Usage: prepare-release.sh <version>}"

echo "$VERSION" > src/main/resources/dude-version

mkdir -p dude/results dude/logs
cp README.md dude/README.md
cp target/dude.jar dude/dude.jar
cp dude.sh dude/dude.sh
cp dude.bat dude/dude.bat
chmod +x dude/dude.sh

zip -r dude.zip dude
