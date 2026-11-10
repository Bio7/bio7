import com.eco.bio7.rbridge.RSetupDialog

/**
 * Opens the setup of R and Rserve (also available in the R preferences):
 * - finds the R installations and selects the R of Bio7
 * - installs the recommended Rserve binary (cooperative mode) of the Bio7 repository
 *   (https://github.com/Bio7/Rserve-Cooperative), chosen by the platform and the version of R
 * - compiles Rserve from the sources for the cooperative mode (needs the build tools)
 * - manual installation: any file of the repository, a local file, the R command to copy,
 *   the target library and the Rserve port/arguments
 */
RSetupDialog.openSetup()
