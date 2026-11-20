#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Usage: prepare-release-voyager.sh <version>}"

echo "$VERSION" > src/main/resources/dude-version

mkdir -p dude/results dude/logs
mkdir -p dude/templates
cp README.md dude/README.md
cp target/dude.jar dude/dude.jar
cp instrument.yml dude/instrument.yml
cp instrument.v2.yml dude/instrument.v2.yml

# The tool's version, from the release tag, in both manifests (V1 and V2)
for f in dude/instrument.yml dude/instrument.v2.yml; do
  awk -v v="$VERSION" '!/^version:/ {print} /^name:/ {print "version: " v}' "$f" > "$f.tmp"
  mv "$f.tmp" "$f"
done

cp dude-summary.py dude/dude-summary.py
cp summary_extract.py dude/summary_extract.py
cp summary_render.py dude/summary_render.py
cp templates/summary.html dude/templates/summary.html
cp languages.yml dude/languages.yml
cp .ignore dude/.ignore

zip -r dude-voyager.zip dude
