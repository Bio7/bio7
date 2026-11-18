#!/bin/bash
# Downloads the JDKs bundled with Bio7 (Eclipse Temurin) and Nashorn (the
# JavaScript engine, as modules on the --module-path of Bio7.product) into the
# platform folders of this feature (<platform>/jdk, not in git). Used by the
# GitHub workflows before the Maven build, and locally to update the JDKs.
#
#   ./fetch_jdks.sh                  all platforms
#   ./fetch_jdks.sh mac_aarch64      one or more platforms
#   BIO7_JDK_RELEASE=jdk-27+35 ./fetch_jdks.sh   another Temurin release
#
# Every download is checked: the JDK against the SHA-256 of Adoptium, the
# Nashorn jars against the SHA-256 below. A JDK of the same release already
# in place is kept.
set -euo pipefail

RELEASE="${BIO7_JDK_RELEASE:-jdk-27.0.1.0.2+1}"
HERE="$(cd "$(dirname "$0")" && pwd)"
API=https://api.adoptium.net/v3
MAVEN=https://repo1.maven.org/maven2

# Nashorn 15.7 and the ASM version it declares (7.3.1), with their SHA-256
NASHORN="org/openjdk/nashorn/nashorn-core/15.7/nashorn-core-15.7.jar 3f2b62e55b5458ba2e8a0cc4599aa3abe81b1422e31c38bb8294a7096ceee6f2
org/ow2/asm/asm/7.3.1/asm-7.3.1.jar 2f67e11ceec819ebd88ddee5300aba699b1cbab2e20c22e97cf027d3be93959b
org/ow2/asm/asm-commons/7.3.1/asm-commons-7.3.1.jar 87cd8bb3c6bf6bcbb33fca48060c5065f66ebf6a3d7de9bf18bff51bcf156ebc
org/ow2/asm/asm-tree/7.3.1/asm-tree-7.3.1.jar f91a4a8aa868c5c4665bb4fd134019a91f9f8b9216527fba295e3c8b5422b78b
org/ow2/asm/asm-analysis/7.3.1/asm-analysis-7.3.1.jar 46b8a8efd4b94facb5ab4b35afe30ee0546ae7a43d2c64e6def56c2f168fefa5
org/ow2/asm/asm-util/7.3.1/asm-util-7.3.1.jar 182128592742ed4883ac82bf205f137b6bfbe1234c68e6feb13759e75a85b729"

sha256() { shasum -a 256 "$1" | cut -d' ' -f1; }

fetch() {   # <platform folder> <Adoptium os> <Adoptium arch> <archive type> <Java home inside jdk/>
	local platform="$1" os="$2" arch="$3" ext="$4" home="$5"
	local dir="$HERE/$platform/jdk" release_file
	release_file="$dir/${home:+$home/}release"
	local version="${RELEASE#jdk-}"
	if [ -f "$release_file" ] && grep -q "^JAVA_RUNTIME_VERSION=\"$version\"" "$release_file"; then
		echo "$platform: $RELEASE is in place"
	else
		local tmp url expected
		tmp="$(mktemp -d)"
		url="$API/binary/version/${RELEASE//+/%2B}/$os/$arch/jdk/hotspot/normal/eclipse"
		expected="$(curl -fsSL "$API/checksum/version/${RELEASE//+/%2B}/$os/$arch/jdk/hotspot/normal/eclipse" | cut -d' ' -f1)"
		echo "$platform: downloading $RELEASE ($os/$arch)"
		curl -fsSL -o "$tmp/jdk.$ext" "$url"
		if [ "$(sha256 "$tmp/jdk.$ext")" != "$expected" ]; then
			echo "$platform: the checksum of the JDK is wrong" >&2
			exit 1
		fi
		mkdir -p "$tmp/x"
		if [ "$ext" = zip ]; then unzip -q "$tmp/jdk.zip" -d "$tmp/x"; else tar -xzf "$tmp/jdk.tar.gz" -C "$tmp/x"; fi
		rm -rf "$dir"
		mkdir -p "$dir"
		cp -R "$tmp"/x/*/. "$dir"/          # the content of jdk-<version>/ (on macOS: Contents/Home)
		rm -rf "$tmp"
	fi
	# Nashorn (always checked: small)
	local nashorn="$dir/${home:+$home/}nashorn"
	mkdir -p "$nashorn"
	while read -r path sum; do
		local jar="$nashorn/$(basename "$path")"
		if [ ! -f "$jar" ] || [ "$(sha256 "$jar")" != "$sum" ]; then
			curl -fsSL -o "$jar" "$MAVEN/$path"
			if [ "$(sha256 "$jar")" != "$sum" ]; then
				echo "$platform: the checksum of $(basename "$path") is wrong" >&2
				exit 1
			fi
		fi
	done <<< "$NASHORN"
	find "$nashorn" -name "asm-*.jar" ! -name "asm*-7.3.1.jar" -delete   # ASM jars of other versions
	cp "$HERE/nashorn/LICENSE" "$nashorn/LICENSE"
}

platforms=("$@")
[ ${#platforms[@]} -gt 0 ] || platforms=(linux_x86_64 linux_aarch64 win_x86_64 mac_aarch64)
for p in "${platforms[@]}"; do
	case "$p" in
	linux_x86_64)  fetch linux_x86_64  linux   x64     tar.gz "" ;;
	linux_aarch64) fetch linux_aarch64 linux   aarch64 tar.gz "" ;;
	win_x86_64)    fetch win_x86_64    windows x64     zip    "" ;;
	mac_aarch64)   fetch mac_aarch64   mac     aarch64 tar.gz Contents/Home ;;
	*) echo "Unknown platform: $p (linux_x86_64, linux_aarch64, win_x86_64, mac_aarch64)" >&2; exit 1 ;;
	esac
done
