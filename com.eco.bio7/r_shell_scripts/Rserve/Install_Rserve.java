import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.Bio7Console;
import com.eco.bio7.image.Util;

public class Install_Rserve {

	public Install_Rserve() {
		boolean install = Bio7Dialog.decision("Do you want to install Rserve?");
		if (install == true) {
			installRServeCurrent();
		}
	}

	public static void installRServeCurrent() {
		String selectionConsole = Bio7Console.getConsoleSelection();
		if (selectionConsole.equals("R")) {
			String OS = Util.getOS();
			String install;
			if (OS.equals("Mac")) {
				install = """
						install.packages("https://raw.github.com/Bio7/Rserve-Cooperative/master/Rserve_1.8-7.tgz", repos = NULL)
												""";

			} else if (OS.equals("Linux")) {
				install = """
						install.packages("https://raw.github.com/Bio7/Rserve-Cooperative/master/Rserve_1.8-7.tar.gz", repos = NULL)
												""";

			} else {
				install = """
						install.packages("https://raw.github.com/Bio7/Rserve-Cooperative/master/Rserve_1.8-7.zip", repos = NULL)
												""";

			}

			Bio7Console.write(install, true, true);
			System.out.println(install);
			System.out.println("Rserve installation routine executed!\n"
			+ "Start Rserve to control a succesful installation!\n");
		} else {
			Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
		}
		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
