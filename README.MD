Minecraft Development IntelliJ Plugin <a href="https://ci.demonwav.com/viewType.html?buildTypeId=MinecraftDevIntelliJ_Build"><img src="https://ci.demonwav.com/app/rest/builds/buildType(id:MinecraftDevIntelliJ_Build)/statusIcon"/></a>
=========================================================================================================================================================================================================================================

Compiling
---------

To compile Minecraft Development IntelliJ Plugin, a few steps need to be
taken to setup an SDK. It is recommended to use Community Edition as the
basis for building this plugin, even if you use Ultimate, as we are
targeting Community Edition to make this available to everyone.

1. [Create the IntelliJ Plugin SDK.](http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/setting_up_environment.html)
2. Once the SDK has been created, go to Project Structure -> SDK's
   (Under Platform Settings). Click on the green + button:
   
   ![IntelliJ SDK](http://i.imgur.com/kn0A7cK.png)
   
   Navigate to the `plugins/maven/lib` directory
   in the IntelliJ installation and add all the Jar's in that directory to
   the classpath. We probably don't need all of them, but it's simpler this
   way.
3. Do the same thing for `plugins/yaml/lib` as well.

Style Guide
-----------

This project will follow DemonWav's Java style guidelines (lol, Google's
style slightly modified). Link [here](http://www.demonwav.com/style).

License
-------

This project is licensed under MIT.
