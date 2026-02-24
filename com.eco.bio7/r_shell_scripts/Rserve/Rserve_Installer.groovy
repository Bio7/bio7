import com.eco.bio7.batch.Bio7Dialog
import com.eco.bio7.console.Bio7Console
import com.eco.bio7.image.Util

/**
 * Script to install Rserve based on OS and Architecture.
 * Improvements: Handles aarch64 (ARM) for Mac/Linux and uses CRAN for Windows.
 */

def installRserve() {
   // The Raw URL of the script you want to execute
	boolean automatically=Bio7Dialog.decision("""
	This script can automatically download and install Rserve from a Github location.

	Do you want to install Rserve from Github with this script?
	""")
	if (!automatically) {	  
 	 	return
	}
	if (Bio7Console.getConsoleSelection() != "R") {
    	Bio7Dialog.message('Please start the "Native R" shell in the Bio7 console!')
    	return
	}

    String os = Util.getOS()
    String arch = System.getProperty("os.arch")
    String installCmd = ""

    // Define the download mapping
    // Note: Windows uses CRAN (repos = "https://cloud.r-project.org")
    // Others use the Bio7 GitHub repository
    switch(os) {
        case "Windows":
            installCmd = 'install.packages("Rserve", repos = "https://cloud.r-project.org")'
            break
            
        case "Mac":
            // Apple aarch64
            String macFile = (arch == "aarch64") ? "Rserve_1.8-15_Mac_arm64.tgz":null
            installCmd = "install.packages(\"https://raw.github.com/Bio7/Rserve-Cooperative/master/$macFile\", repos = NULL)"
            break
            
        case "Linux":
            // Distinguish between x86_64 and ARM64
            String linuxFile = (arch == "aarch64") ? "Rserve_1.8-15_Linux_aarch64.tar.gz" : "Rserve_1.8-15_Linux_x86_64.tar.gz"
            installCmd = "install.packages(\"https://raw.github.com/Bio7/Rserve-Cooperative/master/$linuxFile\", repos = NULL)"
            break
            
        default:
            Bio7Dialog.message("Unsupported OS: $os")
            return
    }

    // Execute in the Bio7 Console
    Bio7Console.write(installCmd, true, true)
    
    println "Executing: $installCmd"
    println "Rserve installation routine sent to console.\n" +
            "Please verify completion in the R shell!"
}

// Execute the function
installRserve()