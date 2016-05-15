Minecraft Development IntelliJ Plugin <a href="https://ci.demonwav.com/viewType.html?buildTypeId=MinecraftDevIntelliJ_Build"><img src="https://ci.demonwav.com/app/rest/builds/buildType(id:MinecraftDevIntelliJ_Build)/statusIcon"/></a>
=========================================================================================================================================================================================================================================

Building
--------

Make sure you have Java 8 and Gradle (version >= 2.1) installed.

Build the plugin with:

`gradle clean buildPlugin`

The output .zip file for the plugin will be in `build/distributions`.

Test the plugin in IntelliJ with:

`gradle clean runIdea`

The [Gradle IntelliJ Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
will handle downloading the IntelliJ dependencies and packaging the
plugin.

Style Guide
-----------

This project will follow DemonWav's Java style guidelines (lol, Google's
style slightly modified). Link [here](http://www.demonwav.com/style).

License
-------

This project is licensed under [MIT](LICENSE.txt).

Supported Platforms
-------------------

- [Bukkit](https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse), [Spigot](https://spigotmc.org/), and [Paper](https://paper.emc.gs)
- [Minecraft Forge](http://minecraftforge.net/forum) and [Sponge](https://www.spongepowered.org/)
- [BungeeCord](https://www.spigotmc.org/wiki/bungeecord/) and [Waterfall](https://getwaterfall.xyz/)
