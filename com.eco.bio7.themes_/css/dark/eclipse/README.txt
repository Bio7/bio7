The Eclipse dark theme, copied unchanged from the bundle org.eclipse.ui.themes
1.3.0.v20260508-0854 (Eclipse 4.41, folder css/dark).

The bundle org.eclipse.ui.themes is not part of Bio7, so the files are copied.
Do not change them, Bio7 changes belong in ../e4-dark_bio7.css (all platforms)
and ../e4-dark_bio7_mac.css (MacOSX).

e4-dark_mac_platform.css, e4-dark_win_platform.css and e4-dark_linux_platform.css
are css/e4-dark_mac.css, css/e4-dark_win.css and css/e4-dark_linux.css of
org.eclipse.ui.themes without the @import lines (the imports are in the Bio7
files css/e4-dark_mac.css, css/e4-dark_win.css and css/e4-dark.css).

e4-dark_preferencestyle.css is in ../ (with the Bio7 editor background).

To update: copy the files of a newer org.eclipse.ui.themes again, remove the
@import lines of the platform files and merge the preferences of
e4-dark_preferencestyle.css into ../e4-dark_preferencestyle.css.
