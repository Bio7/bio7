

/*
This example demonstrates the use of JavaFX embedded in a custom view of Bio7 for dynamic data.
For a successful compilation switch to the full class(es) compilation in the Java preferences of Bio7.

After the compilation a custom view will be opened.
 
Invoke the Start/Stop action in the main toolbar to plot random
values dynamically!

*/
import javafx.application.Platform;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.collection.CustomView;
import java.io.*;

public class My_Model extends com.eco.bio7.compile.Model {

	_PleaseProvideControllerClassName p = new _PleaseProvideControllerClassName();

	public  My_Model() {

		CustomView view = new CustomView();
        
		view.setFxmlCanvas("dynamic", FileRoot.getCurrentCompileDir()+"/_My_Model.fxml", p);

	}
	
	public void setup(){
		/*Reset the chart!*/
		p.reset();
		/*Reset the counter!*/
		Bio7Action.reset();
	}

	public void run() {

		Platform.runLater(new Runnable() {

			public void run() {
				p.dynamic();
			}
		});
	}
}