#!/bin/bash
# Packages the macOS apps of a Bio7 build for other Macs (run on a Mac after
# "mvn clean install"): each Bio7.app in target/products is prepared
# (prepare_macos_app.sh: camera usage descriptions, workspace outside of the
# app, signature with the entitlements), optionally notarized, and archived:
#
#   target/products/bio7-macosx.cocoa.<arch>.tar.gz   (replaces the archive of Tycho)
#   target/products/bio7-macosx.cocoa.<arch>.dmg      (a disk image with an Applications link)
#
#   ./package_macos.sh [target/products]
#   ./package_macos.sh /path/to/Bio7.app    one app (e.g., an installed one; quit
#                                            Bio7 first): the archives next to it
#
# Without a certificate the apps are signed ad hoc: they run on other Macs, but
# macOS blocks a downloaded app first (System Settings > Privacy & Security >
# "Open Anyway", or: xattr -dr com.apple.quarantine /Applications/Bio7.app).
#
# With an Apple Developer ID (no warning on other Macs):
#   BIO7_SIGN_IDENTITY="Developer ID Application: Name (TEAMID)" \
#   BIO7_NOTARY_PROFILE=bio7 ./package_macos.sh
# BIO7_NOTARY_PROFILE: a keychain profile of notarytool, created once with
#   xcrun notarytool store-credentials bio7 --apple-id <id> --team-id <TEAMID>
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
TARGET="${1:-$HERE/../target/products}"
TARGET="${TARGET%/}"
IDENTITY="${BIO7_SIGN_IDENTITY:--}"
NOTARY="${BIO7_NOTARY_PROFILE:-}"
if [ -n "$NOTARY" ] && [ "$IDENTITY" = "-" ]; then
	echo "The notarization needs a Developer ID signature (BIO7_SIGN_IDENTITY)." >&2
	exit 1
fi

shopt -s nullglob
if [[ "$TARGET" == *.app ]]; then
	# One app: the archives in its folder
	[ -d "$TARGET" ] || { echo "No app: $TARGET" >&2; exit 1; }
	apps=("$(cd "$TARGET" && pwd)")
	PRODUCTS="$(dirname "${apps[0]}")"
else
	PRODUCTS="$(cd "$TARGET" && pwd)"
	apps=("$PRODUCTS"/*/macosx/cocoa/*/Bio7.app)
	if [ ${#apps[@]} -eq 0 ]; then
		echo "No macOS app in $PRODUCTS (build first: mvn clean install)." >&2
		exit 1
	fi
fi

notarize() {   # a file (zip or dmg): submitted, waited for, the ticket stapled (dmg)
	xcrun notarytool submit "$1" --keychain-profile "$NOTARY" --wait
}

for APP in "${apps[@]}"; do
	ARCH="$(basename "$(dirname "$APP")")"
	case "$ARCH" in
	aarch64 | x86_64) ;;
	*)	# an app outside of the build: the architecture of its launcher
		ARCH="$(lipo -archs "$APP/Contents/MacOS/Bio7" 2>/dev/null || echo unknown)"
		ARCH="${ARCH/arm64/aarch64}"
		ARCH="${ARCH// /-}" ;;
	esac
	NAME="bio7-macosx.cocoa.$ARCH"
	echo "=== $ARCH: $APP"
	"$HERE/prepare_macos_app.sh" "$APP"

	if [ -n "$NOTARY" ]; then
		ZIP="$(mktemp -d)/Bio7.zip"
		ditto -c -k --keepParent "$APP" "$ZIP"
		notarize "$ZIP"
		xcrun stapler staple "$APP"           # the ticket in the app: works offline
		rm -rf "$(dirname "$ZIP")"
	fi

	# The archive (as the one of Tycho: Bio7.app at the top)
	tar -czf "$PRODUCTS/$NAME.tar.gz" -C "$(dirname "$APP")" Bio7.app

	# The disk image: Bio7.app and a link to /Applications (drag to install)
	STAGE="$(mktemp -d)"
	ditto "$APP" "$STAGE/Bio7.app"
	ln -s /Applications "$STAGE/Applications"
	rm -f "$PRODUCTS/$NAME.dmg"
	hdiutil create -quiet -volname "Bio7" -srcfolder "$STAGE" -fs HFS+ -format UDZO "$PRODUCTS/$NAME.dmg"
	rm -rf "$STAGE"
	if [ "$IDENTITY" != "-" ]; then
		codesign --force --timestamp --sign "$IDENTITY" "$PRODUCTS/$NAME.dmg"
		if [ -n "$NOTARY" ]; then
			notarize "$PRODUCTS/$NAME.dmg"
			xcrun stapler staple "$PRODUCTS/$NAME.dmg"
		fi
	fi
	echo "Packaged: $PRODUCTS/$NAME.tar.gz, $PRODUCTS/$NAME.dmg"
done
