# Bio7 for macOS

The build (Tycho) changes the app after the launcher was signed, so a built
`Bio7.app` has an invalid signature and no camera permission texts: macOS
refuses the camera without asking. These scripts make the app ready for macOS
(run them on a Mac).

| Script | |
|---|---|
| `package_macos.sh` | After `mvn clean install`: prepares each `Bio7.app` in `target/products`, optionally notarizes it, and creates `bio7-macosx.cocoa.<arch>.tar.gz` (replaces the archive of Tycho) and `bio7-macosx.cocoa.<arch>.dmg` |
| `prepare_macos_app.sh Bio7.app` | Prepares one app (also an installed one, Bio7 must be quit): bundle identifier `com.eco.bio7`, camera and microphone usage descriptions, a workspace in the app moved to `~/Bio7/workspace`, signed again with `bio7.entitlements` |
| `bio7.entitlements` | The entitlements of the Eclipse launcher plus the camera and the microphone |

## Signing

* Ad hoc (default): works on every Mac, but macOS blocks a downloaded app the
  first time: System Settings > Privacy & Security > "Open Anyway" (or
  `xattr -dr com.apple.quarantine /Applications/Bio7.app`).
* Developer ID and notarization (an Apple Developer account): no warning.

```
xcrun notarytool store-credentials bio7 --apple-id <apple id> --team-id <TEAMID>   # once
BIO7_SIGN_IDENTITY="Developer ID Application: Name (TEAMID)" BIO7_NOTARY_PROFILE=bio7 ./package_macos.sh
```

## The camera

Start Bio7 from the Finder or the Dock (not from a terminal: macOS would ask
for the terminal). The first time the camera is opened macOS asks for the
permission; then Bio7 is listed in System Settings > Privacy & Security >
Camera.

The workspace of Bio7 on macOS is `~/Bio7/workspace` (Bio7.product:
`osgi.instance.area.default`): inside the app it would break the signature.
