IntelliJBukkitSupport
=====================

Compiling
---------

To compile IntelliJBukkitSupport, a few steps need to be taken to setup an SDK.

1. Create a zip of the contents of the latest (version 15.0.1 in this case) IntelliJ folder.<br>
On windows this folder could be: `C:\Program Files (x86)\JetBrains\IntelliJ IDEA 15.0`<br>
Make sure the name of the file is `IntelliJ.zip`
2. Install the file to your local repository by running<br>
`$ mvn install:install-file -Dfile=IntelliJ.zip -DgroupId=org.jetbrains -DartifactId=intellij-idea-iu -Dversion=15.0.1 -Dpackaging=zip`
3. Now compile the project using `$ mvn package`

Running with IntelliJ IDEA
--------------------------
To run this plugin with IntelliJ to debug. You must install a plugin called: "Intellij plugin development with Maven"