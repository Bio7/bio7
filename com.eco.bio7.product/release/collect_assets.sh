#!/bin/bash
# Collects the release files of a Bio7 build (after "mvn install" and, on a
# Mac, ../macos/package_macos.sh) with the version in their names, and writes
# their SHA-256 checksums:
#
#   ./collect_assets.sh <version> <output folder>
#
#   bio7-<version>-linux-x86_64.tar.gz
#   bio7-<version>-linux-aarch64.tar.gz
#   bio7-<version>-windows-x86_64.zip
#   bio7-<version>-macos-aarch64.dmg
#   bio7-<version>-macos-aarch64.tar.gz
#   SHA256SUMS.txt
#   LICENSE.txt, THIRD-PARTY-NOTICES.md
set -euo pipefail

VERSION="${1:?usage: $0 <version> <output folder>}"
OUT="${2:?usage: $0 <version> <output folder>}"
HERE="$(cd "$(dirname "$0")" && pwd)"
PRODUCTS="$HERE/../target/products"

mkdir -p "$OUT"
copy() {   # <file of the build> <name of the release file>
	if [ ! -f "$PRODUCTS/$1" ]; then
		echo "Missing in the build: $PRODUCTS/$1" >&2
		exit 1
	fi
	cp "$PRODUCTS/$1" "$OUT/$2"
	echo "$2"
}
copy bio7-linux.gtk.x86_64.tar.gz      "bio7-$VERSION-linux-x86_64.tar.gz"
copy bio7-linux.gtk.aarch64.tar.gz     "bio7-$VERSION-linux-aarch64.tar.gz"
copy bio7-win32.win32.x86_64.zip       "bio7-$VERSION-windows-x86_64.zip"
copy bio7-macosx.cocoa.aarch64.dmg     "bio7-$VERSION-macos-aarch64.dmg"
copy bio7-macosx.cocoa.aarch64.tar.gz  "bio7-$VERSION-macos-aarch64.tar.gz"

# The license and the notices of the third-party components (also in every download)
cp "$HERE/../../LICENSE" "$OUT/LICENSE.txt"
cp "$HERE/../../THIRD-PARTY-NOTICES.md" "$OUT/THIRD-PARTY-NOTICES.md"
echo LICENSE.txt
echo THIRD-PARTY-NOTICES.md

# The checksums (check: shasum -a 256 -c SHA256SUMS.txt)
(cd "$OUT" && shasum -a 256 bio7-"$VERSION"-* > SHA256SUMS.txt)
echo SHA256SUMS.txt
