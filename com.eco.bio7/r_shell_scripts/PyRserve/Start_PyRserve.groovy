import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.console.Bio7Console
import com.eco.bio7.rbridge.RServe;

/*If Rserve is running we stop it to return to the native R connection!*/
if (RServe.isAlive()){
	Bio7Action.callRserve();// Change to native connection!
}
/*Change to the Bio7 R interpreter of the Bio7 console!*/
Bio7Console.setConsoleSelection("R");
/*Write a linebreak!*/
Bio7Console.write("\n", true, false);
/*Start Rserve independently from the default Bio7 action (shutdowns Rserve) in the native R connection!*/
Bio7Console.write("library(Rserve);run.Rserve()", true, false);
/*We need a sleep time here to establish the connection!*/
Thread.sleep(1000);

