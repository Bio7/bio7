#!/bin/bash
# Prepares Bio7.app (built by Tycho) for macOS: a bundle identifier, the
# texts macOS shows when Bio7 asks for the camera and the microphone, and a
# new signature with the entitlements (the camera needs one with the hardened
# runtime). Without this macOS refuses the camera without asking and Bio7 is
# not listed in System Settings > Privacy & Security > Camera: the build
# changes the app after the launcher was signed, so its signature is invalid.
#
#   ./prepare_macos_app.sh /path/to/Bio7.app
#
# Signing identity: ad hoc ("-", for this computer) or yours, e.g.
#   BIO7_SIGN_IDENTITY="Developer ID Application: Name (TEAMID)" ./prepare_macos_app.sh Bio7.app
# (for other computers: a Developer ID signature and notarization).
# Quit Bio7 before (the app is signed again).
set -euo pipefail

APP="${1:?usage: $0 /path/to/Bio7.app}"
HERE="$(cd "$(dirname "$0")" && pwd)"
PLIST="$APP/Contents/Info.plist"
IDENTITY="${BIO7_SIGN_IDENTITY:--}"
[ -f "$PLIST" ] || { echo "Not an app: $APP" >&2; exit 1; }

# Bio7 must not run (the app is changed and signed again)
if pgrep -f "$APP/Contents/MacOS/Bio7" >/dev/null 2>&1; then
	echo "Please quit Bio7 first (it runs from $APP)." >&2
	exit 1
fi

# The workspace must not be in the app: codesign refuses files in Contents/MacOS,
# and Bio7 writing into it would change the signed app. It is moved to
# ~/Bio7/workspace and Bio7 uses that location (config.ini).
WORKSPACE="$HOME/Bio7/workspace"
if [ -d "$APP/Contents/MacOS/workspace" ]; then
	if [ -e "$WORKSPACE" ]; then
		echo "The app contains a workspace, and $WORKSPACE exists already." >&2
		echo "Move or merge $APP/Contents/MacOS/workspace yourself, then run this again." >&2
		exit 1
	fi
	mkdir -p "$(dirname "$WORKSPACE")"
	mv "$APP/Contents/MacOS/workspace" "$WORKSPACE"
	echo "The workspace was moved to $WORKSPACE"
fi
CONFIG="$APP/Contents/Eclipse/configuration/config.ini"
if [ -f "$CONFIG" ] && ! grep -q "^osgi.instance.area.default=" "$CONFIG"; then
	printf '\nosgi.instance.area.default=@user.home/Bio7/workspace\n' >> "$CONFIG"
fi
EXTRA=$(find "$APP/Contents/MacOS" -mindepth 1 ! -name Bio7 | head -3)
if [ -n "$EXTRA" ]; then
	echo "Contents/MacOS may only contain the launcher (codesign refuses other files):" >&2
	echo "$EXTRA" >&2
	exit 1
fi

set_key() {   # key, type, value: set or add
	/usr/libexec/PlistBuddy -c "Set :$1 $3" "$PLIST" 2>/dev/null || /usr/libexec/PlistBuddy -c "Add :$1 $2 $3" "$PLIST"
}
set_key CFBundleIdentifier string "com.eco.bio7"
set_key NSCameraUsageDescription string "Bio7 records and analyzes images of the camera (e.g., OpenCV in Python, ImageJ)."
set_key NSMicrophoneUsageDescription string "Bio7 records audio for analyses (e.g., in Python)."

# Extended attributes (e.g., quarantine, Finder info) would break the signature
xattr -cr "$APP"

# Signed again: all nested code (JRE, native libraries) and the app
# (a Developer ID signature with a secure timestamp: needed for the notarization)
if [ "$IDENTITY" = "-" ]; then TIMESTAMP="--timestamp=none"; else TIMESTAMP="--timestamp"; fi
codesign --force --deep --options runtime $TIMESTAMP --entitlements "$HERE/bio7.entitlements" --sign "$IDENTITY" "$APP"
codesign --verify --deep --strict "$APP"
echo "Prepared: $APP (identifier com.eco.bio7, camera allowed, signed with '${IDENTITY}')"
echo "Start Bio7 and open the camera: macOS asks once; then Bio7 is listed in Privacy & Security > Camera."
