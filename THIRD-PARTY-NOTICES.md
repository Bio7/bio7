# Third-Party Notices

Bio7 is licensed under the Eclipse Public License 2.0 (see `LICENSE`). It
includes the third-party components listed below, each under its own license.
The license texts are in the components themselves (in their jar files, folders
or source headers) or at the links given.

Components under a copyleft license (LGPL, MPL, CDDL, GPL with Classpath
Exception) are included unmodified, as separate libraries. Their source code is
available at the links in the section "Source code of copyleft components".

## Java runtime

| Component | Version | License | Source |
|---|---|---|---|
| Eclipse Temurin JDK (OpenJDK) | 27.0.1 | GPL-2.0 with Classpath Exception | https://github.com/adoptium/jdk27u |
| Nashorn (JavaScript engine) | 15.7 | GPL-2.0 with Classpath Exception | https://github.com/openjdk/nashorn |
| ASM (used by Nashorn) | 7.3.1 | BSD-3-Clause | https://gitlab.ow2.org/asm/asm |

The JDK is bundled in the folder `jdk`, Nashorn in `jdk/nashorn` (macOS:
`jdk/Contents/Home/nashorn`), each with its license.

## Eclipse platform and ImageJ

| Component | License | Source |
|---|---|---|
| Eclipse Platform, SWT, JFace, Equinox, JDT, GEF, Nebula and other Eclipse projects | EPL-2.0 (some EPL-1.0) | https://github.com/eclipse-platform |
| SWT ImageJ (ImageJ for SWT) | EPL-2.0 | https://github.com/eclipse-swtimagej |
| ImageJ 1.x | Public domain | https://imagej.net/ij/ |
| SWTImageJ LLM Assistant (ImageJ plugin) | Apache-2.0 | https://github.com/eclipse-swtimagej |

## Libraries

| Library | License |
|---|---|
| Apache Ant, Commons (BeanUtils, Codec, Collections, Compress, IO, Lang, Logging, Math, Text), Ivy, Log4j API, POI, XMLBeans | Apache-2.0 |
| Apache Groovy 6 (and its modules), picocli, FlatLaf, JSVG, Jansi | Apache-2.0 |
| Guava, Jackson 2 (annotations, core, databind, YAML, JSR-310), SnakeYAML, opencsv, SparseBitSet, Kotlin standard library | Apache-2.0 |
| JUnit 5, JUnit 4, opentest4j | EPL-2.0 / EPL-1.0 |
| JTS Topology Suite | EPL-2.0 / EDL-1.0 |
| pty4j | EPL-1.0 |
| Jericho HTML Parser | EPL-1.0 / LGPL-2.1 / Apache-2.0 |
| ANTLR 4 runtime, Janino (commons-compiler), dom4j, JLine, Hamcrest, abego TreeLayout, flexmark, commonmark, autolink | BSD-style |
| JOGL, GlueGen, JOCL (JogAmp, including the native libraries) | BSD-style |
| XStream, curvesapi | BSD-style |
| mXparser | Indiana University Extreme! Lab Software License (BSD-style) |
| Parallel Colt | BSD-style (CERN Colt license) |
| JOML, jsoup, SLF4J | MIT |
| JBullet, stack-alloc | zlib |
| Jama, XmlPull | Public domain |
| BeanShell 2.0b6 | Apache-2.0 |
| JNA, jna-platform | LGPL-2.1 or Apache-2.0 |
| javaparser | LGPL-3.0 or Apache-2.0 |
| jSerialComm | LGPL-3.0 or Apache-2.0 |
| jSSC | LGPL-3.0 or BSD |
| LibreOffice / OpenOffice.org UNO (java_uno, juh, jurt, ridl, unoil, unoloader, wizards) | MPL-2.0 / LGPL-3.0 |
| JavaBeans Activation Framework | CDDL-1.0 |
| JAXB (api, core, impl) | CDDL-1.1 or GPL-2.0 with Classpath Exception |
| metadata-extractor, Adobe XMP Core (in the ImageJ plugin Exif_Reader) | Apache-2.0 / BSD |
| WindowBuilder GroupLayout and SWT resource managers | EPL-1.0 / free distribution |

## Native programs and libraries

| Component | License | Source |
|---|---|---|
| jSSC, NRJavaSerial native libraries (Windows) | LGPL-3.0 | https://github.com/java-native/jssc, https://github.com/NeuronRobotics/nrjavaserial |

## Source code included in Bio7

| Code | Origin | License |
|---|---|---|
| NASA WorldWind Java 2.2 (`com.eco.bio7.WorldWind`, `gov.nasa.worldwind`) | NASA | NASA Open Source Agreement 1.3 (see `com.eco.bio7.WorldWind/LICENSE.html`); WorldWind Java 2.1 and later are also available under Apache-2.0 |
| Jackson 1.x (`org.codehaus.jackson`, in the WorldWind plugin) | FasterXML / Tatu Saloranta | Apache-2.0 |
| WorldWind NEWT/SWT classes | Geoscience Australia | Apache-2.0 |
| Flow editor rulers and guides (`com.eco.bio7.floweditor.ruler`) | Adapted from the GEF Logic example (IBM) by uniera.org | EPL-2.0 |
| Python partition scanner (`PyPartitionScanner`) | PyDev (Aleks Totic) | CPL-1.0 |
| Janino commons (`org.codehaus.commons`) | Arno Unkrig | BSD-3-Clause |
| R and ImageJ macro grammars | ANTLR grammars (Terence Parr, Bart Kiers) | BSD-3-Clause / MIT |
| Process management (`UnixProcessManager`) | JetBrains | Apache-2.0 |
| Windows process API (`W32API`, `W32Errors`) | JNA (Timothy Wall) | LGPL-2.1 or Apache-2.0 |
| Levenshtein distance, text dialog | Apache Commons | Apache-2.0 |
| `FPSAnimator` | Sun Microsystems (JOGL) | BSD-style |
| Hover information control | Dan Breslau | Public domain |

## Source code of copyleft components

The following components are under a copyleft license. They are included
unmodified; their complete source code is available here:

| Component | License | Source code |
|---|---|---|
| Eclipse Temurin JDK 27 | GPL-2.0 with Classpath Exception | https://github.com/adoptium/jdk27u |
| Nashorn 15.7 | GPL-2.0 with Classpath Exception | https://github.com/openjdk/nashorn |
| LibreOffice UNO libraries | MPL-2.0 / LGPL-3.0 | https://git.libreoffice.org/core |
| JNA | LGPL-2.1 or Apache-2.0 | https://github.com/java-native-access/jna |
| javaparser | LGPL-3.0 or Apache-2.0 | https://github.com/javaparser/javaparser |
| jSerialComm | LGPL-3.0 or Apache-2.0 | https://github.com/Fazecast/jSerialComm |
| jSSC | LGPL-3.0 or BSD | https://github.com/java-native/jssc |
| NRJavaSerial | LGPL-3.0 | https://github.com/NeuronRobotics/nrjavaserial |
| JavaBeans Activation Framework | CDDL-1.0 | https://github.com/javaee/activation |
| JAXB | CDDL-1.1 or GPL-2.0 with Classpath Exception | https://github.com/javaee/jaxb-v2 |

## Not included

Bio7 can use R, Python, GDAL and OpenCV, but doesn't include them: they are
installed by the user and are under their own licenses (R: GPL-2.0/3.0, Python:
PSF License, GDAL: MIT, OpenCV: Apache-2.0).
