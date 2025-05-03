## Bio7
The application Bio7 is an integrated development environment for ecological modeling, statistical and scientific image analysis. The application itself is based on an RCP-Eclipse-Environment (Rich-Client-Platform) which offers a huge flexibility in configuration and extensibility because of its plug-in structure and the possibility of customization.

[![Bio7 Overview Video](https://raw.githubusercontent.com/Bio7/bio7/master/resources/screen.jpg)](https://www.youtube.com/watch?v=pyYn690KaNE)


### [Website: https://bio7.org](https://bio7.org).

### [YouTube Channel Bio7](https://www.youtube.com/channel/UCFY-w-tMbVzhrLro4Q2KbFg)

Features:

* Creation and analysis of simulation models.
* Statistical analysis.
* Advanced R Graphical User Interface with editor, spreadsheet, ImageJ plot device and debugging interface.
* Spatial statistics (possibility to send values from a specialized panel to R).
* Image Analysis (embedded ImageJ).
* Fast transfer of image data from ImageJ to R and vice versa.
* Fast communication between R and Java (with RServe) and the possibility to use R methods inside Java.
* Interpretation of Java and script creation (BeanShell, Groovy, Jython, JavaScript).
* Dynamic compilation of Java.
* Creation of methods for Java, BeanShell, Groovy, Jython and R (integrated editors for Java, R, BeanShell, Groovy, Jython).
* Sensitivity analysis with an embedded flowchart editor in which scripts, macros and compiled code can be dragged and executed.
* Creation of 3d OpenGL (Jogl) models.
* Visualizations and simulations on an embedded 3d globe (World Wind Java SDK).
* Creation of Graphical User Interfaces with the embedded JavaFX SceneBuilder.

### Built Bio7

Bio7 is now available as a Maven Tycho built and can be created locally. All plugins and features are now available in one Github repository.

#### To built Bio7:

1. For the built Eclipse must be installed locally (please use this distribution: Eclipse IDE for RCP and RAP Developers).

2. Download and install Java 21 JDK (Adoptium JDK can be downloaded here:  https://adoptium.net).

3. Download and install the latest Maven release (https://maven.apache.org/).

4. Import all Plugins and Features from the bio7 Git repository (https://github.com/Bio7/bio7).

5. Expand the com.eco.bio7.targetplatform plugin. Execute "Set as Active Target Platform" and then execute Reload Target Platform". See also: https://www.vogella.com/tutorials/EclipseTargetPlatform/article.html

6. Open a shell and navigate to the main Maven *.pom in the Git repository (in the downloaded Git repository, e.g., on MacOSX: 'cd /Users/xxxx/git/bio7/').

7. Execute the parent *.pom in the shell with: 'mvn -f com.eco.bio7.aggregator/pom.xml clean install' (or with 4 cores available: 'mvn -T 4 -f com.eco.bio7.aggregator/pom.xml clean install').

8. The Bio7 built is now available in the com.eco.bio7.product plugin (in the targets/products folder - refresh the folder if not visible!).

To start Bio7 with a locally bundled JDK navigate to the Bio7 folder and change the Bio7.ini file (add a -vm option which points to a JDK, see: https://wiki.eclipse.org/Eclipse.ini

To start Bio7 in Eclipse navigate to the com.eco.bio7.product plugin and open the Bio7.product file. Then execute the "Launch an Eclipse application" action.

Links: 

https://www.vogella.com/tutorials/EclipseTycho/article.html

https://tycho.eclipseprojects.io/doc/latest/

