## Downloads

| System | File |
|---|---|
| macOS (Apple Silicon) | `bio7-VERSION-macos-aarch64.dmg`: open it and drag Bio7 to Applications |
| Windows (x86_64) | `bio7-VERSION-windows-x86_64.zip`: unzip and start `Bio7.exe` |
| Linux (x86_64, aarch64) | `bio7-VERSION-linux-<arch>.tar.gz`: unpack and start `Bio7` |

Bio7 includes its Java runtime (Eclipse Temurin 27 with Nashorn): no Java installation is needed.
The checksums of all files are in `SHA256SUMS.txt` (`shasum -a 256 -c SHA256SUMS.txt`).
Bio7 is licensed under the EPL-2.0 (`LICENSE.txt`); the included third-party components
and their licenses are listed in `THIRD-PARTY-NOTICES.md` (also in every download).

### macOS: the first start

Bio7 is not notarized by Apple, so macOS blocks it the first time:
"Apple could not verify 'Bio7' is free of malware". Open **System Settings >
Privacy & Security**, scroll down to the message about Bio7 and click **Open
Anyway** (once per version). Or in the Terminal:

    xattr -dr com.apple.quarantine /Applications/Bio7.app

### Windows: the first start

Bio7 is not signed with a Windows certificate, so SmartScreen may warn:
click **More info > Run anyway**.

### macOS: the camera

The camera (e.g., OpenCV in Python): macOS asks for the permission the first
time; start Bio7 from the Finder or the Dock, not from a terminal. The
workspace is `~/Bio7/workspace`.

## Changes
