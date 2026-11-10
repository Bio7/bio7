# com.eco.bio7.rffm: embedded R with the Foreign Function and Memory API

Loads an existing R installation (built as a shared library: the CRAN builds for
Windows and macOS, the Linux distributions, rig) into the Java process and calls
R with the Foreign Function and Memory API of Java 22+. No Rserve, no JNI, no
compiled native code. The bundle depends only on the JDK (no Eclipse, no SWT),
so it can be used by any RCP, SWT, Swing or plain Java application.

Status: experimental.

## Requirements

* Java 22 or later, started with `--enable-native-access=ALL-UNNAMED`
  (OSGi bundles are unnamed modules).
* An R installation with `lib/libR.dylib` (macOS), `lib/libR.so` (Linux) or
  `bin/x64/R.dll` (Windows).

## Usage

```java
RSession r = RSession.start(RStartOptions.of(Path.of("/Library/Frameworks/R.framework/Resources"))
        .withConsole((text, error) -> System.out.print(text)));
double[] x = r.evalDoubles("rnorm(10)");
r.assign("m", new double[][] { { 1, 2 }, { 3, 4 } });
RValue v = r.eval("summary(lm(dist ~ speed, cars))$coefficients");
r.console("plot(1:10)");             // like typed in the R console (printing, warnings, errors)
r.interrupt();                       // like Ctrl+C
```

`RValue` is the data model: one record per R vector type (`Num`, `Int`,
`Logical`, `Str`, `Raw`, `List`, `Null`, `Other`) with the attributes names,
dim, dimnames, class, levels and row.names. NA values are kept as in R.

## Graphics devices

`RGraphicsDevice` is a device implemented in Java; `Java2DGraphicsDevice` draws
with Java2D into an image and delivers frames while R draws (and when it
stops), so an application can show plots live:

```java
Java2DGraphicsDevice dev = new Java2DGraphicsDevice(640, 480, 96, 2.0);
dev.setFrameListener(frame -> display(frame));      // hand to the UI asynchronously
r.registerDevice("javaDevice", (w, h, ps) -> new RDeviceFactory.DeviceSpec(dev, 96, ps));
r.voidEval("options(device = 'javaDevice'); plot(1:10)");
r.resizeDevice(dev, 800, 600);                     // replays the plot in the new size
```

## Limits of embedding

* One R per process; after `close()` R can't be started again.
* A crash of R (e.g., of a package) ends the process.
* R is single-threaded: calls wait while R evaluates or waits for input.
* `q()` raises an error (it would end the process) unless `allowQuit` is set.
* The quartz device of macOS needs the main thread (owned by SWT): use a Java
  device or file devices instead.
* Callbacks of R (devices, console) must not wait for the UI thread.

R errors never reach Java frames: all evaluation runs through `R_tryEval`, and
the parsing of the console is done in R.
