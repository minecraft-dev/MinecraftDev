Minecraft Development IntelliJ Plugin [![forthebadge](http://forthebadge.com/images/badges/made-with-crayons.svg)](http://forthebadge.com)
==========================================================================================================================================
[![GitHub stars](https://img.shields.io/github/stars/DemonWav/MinecraftDevIntelliJ.svg)](https://github.com/DemonWav/MinecraftDevIntelliJ/stargazers) [![GitHub issues](https://img.shields.io/github/issues/DemonWav/MinecraftDevIntelliJ.svg)](https://github.com/DemonWav/MinecraftDevIntelliJ/issues) <a href="https://ci.demonwav.com/viewType.html?buildTypeId=MinecraftDevIntelliJ_Build"><img src="https://ci.demonwav.com/app/rest/builds/buildType(id:MinecraftDevIntelliJ_Build)/statusIcon"/></a>

Building
--------

Make sure you have Java 8 installed.

Build the plugin with:

`./gradle clean buildPlugin`

The output .zip file for the plugin will be in `build/distributions`.

Test the plugin in IntelliJ with:

`./gradle clean runIdea`

The [Gradle IntelliJ Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
will handle downloading the IntelliJ dependencies and packaging the
plugin.

Style Guide
-----------

This project will follow DemonWav's Java style guidelines (lol, Google's
style slightly modified). Link [here](http://www.demonwav.com/style).

Developers
----------

[**@DemonWav** - Kyle Wood](https://github.com/DemonWav)

#### **Contributors**

- [**@gabizou** - Gabriel Harris-Rouquette](https://github.com/gabizou)
- [**@kashike**](https://github.com/kashike)

License
-------

This project is licensed under [MIT](LICENSE.txt).

Supported Platforms
-------------------

- [Bukkit](https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse), [Spigot](https://spigotmc.org/), and [Paper](https://paper.emc.gs)
- [Sponge](https://www.spongepowered.org/) and [Minecraft Forge](http://minecraftforge.net/forum)
- [BungeeCord](https://www.spigotmc.org/wiki/bungeecord/)
