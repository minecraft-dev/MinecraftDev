IntelliJBukkitSupport
=====================

Compiling
---------

To compile IntelliJBukkitSupport, a few steps need to be taken to setup an SDK.

1. Create the IntelliJ Plugin SDK (just follow the steps when you create a new SDK). You must use IntelliJ Ultimate as
we provide special support for IntelliJ Ultimate's YAML plugin.
2. Once the SDK has been created, go to Project Structure -> SDK's (Under Platform Settings). Navigate to the
`plugins/maven/lib` directory in the IntelliJ installation and add all the Jar's in that directory to the classpath.
We probably don't need all of them, but it's simpler this way.
3. Do the same thing for `plugins/yaml/lib` as well.

Style Guide
-----------

This project will follow DemonWav's Java style guidelines (lol, Google's style slightly modified). Link
[here](http://www.demonwav.com/style).

License
-------

This project is licensed under MIT.
