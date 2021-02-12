<p align="center"><a href="https://minecraftdev.org/"><img src="https://minecraftdev.org/assets/icon.svg" height="120" alt="logo"/></a></p>

Minecraft Development for IntelliJ
==================================

<table>
    <tr>
        <td align="center" colspan="3"><b>Build Status</b></td>
    </tr>
    <tr>
        <td align="right"><b>Main Build</b></td>
        <td colspan="2"><a href="https://ci.demonwav.com/viewType.html?buildTypeId=MinecraftDev_Build"><img src="https://tc.demonwav.com/app/rest/builds/buildType:(id:MinecraftDev_Build)/statusIcon.svg" alt="Teamcity Build Status" /></a></td>
    </tr>
    <tr>
        <td align="right" rowspan="4" ><b>Nightly Builds</b></td>
        <td align="left">2019.3</td>
        <td align="left"><a href="https://ci.demonwav.com/viewType.html?buildTypeId=MinecraftDev_Nightly_20193"><img src="https://tc.demonwav.com/app/rest/builds/buildType:(id:MinecraftDev_Nightly_20193)/statusIcon.svg" alt="2019.3 Nightly Status" /></a></td>
    </tr>
    <tr>
        <td align="left">2020.1</td>
        <td align="left"><a href="https://ci.demonwav.com/viewType.html?buildTypeId=MinecraftDev_Nightly_20201"><img src="https://tc.demonwav.com/app/rest/builds/buildType:(id:MinecraftDev_Nightly_20201)/statusIcon.svg" alt="2020.1 Nightly Status" /></a></td>
    </tr>
    <tr>
        <td align="left">2020.2</td>
        <td align="left"><a href="https://ci.demonwav.com/viewType.html?buildTypeId=MinecraftDev_Nightly_20202"><img src="https://tc.demonwav.com/app/rest/builds/buildType:(id:MinecraftDev_Nightly_20202)/statusIcon.svg" alt="2020.2 Nightly Status" /></a></td>
    </tr>
    <tr>
        <td align="left">2020.3</td>
        <td align="left"><a href="https://ci.demonwav.com/viewType.html?buildTypeId=MinecraftDev_Nightly_20203"><img src="https://tc.demonwav.com/app/rest/builds/buildType:(id:MinecraftDev_Nightly_20203)/statusIcon.svg" alt="2020.3 Nightly Status" /></a></td>
    </tr>
    <tr>
        <td align="right" rowspan="3"><b>OS Tests</b></td>
        <td align="left" colspan="2">
            <a href="https://github.com/minecraft-dev/MinecraftDev/actions?query=workflow%3A%22Linux+Build%22"><img src="https://github.com/minecraft-dev/MinecraftDev/workflows/Linux%20Build/badge.svg?branch=dev&event=push" alt="Linux GitHub Action Status" /></a>
            <br/>
            <a href="https://github.com/minecraft-dev/MinecraftDev/actions?query=workflow%3A%22macOS+Build%22"><img src="https://github.com/minecraft-dev/MinecraftDev/workflows/macOS%20Build/badge.svg?branch=dev&event=push" alt="macOS GitHub Action Status" /></a>
            <br/>
            <a href="https://github.com/minecraft-dev/MinecraftDev/actions?query=workflow%3A%22Windows+Build%22"><img src="https://github.com/minecraft-dev/MinecraftDev/workflows/Windows%20Build/badge.svg?branch=dev&event=push" alt="Windows GitHub Action Status" /></a>        
        </td>
    </tr>
</table>

Info and Documentation [![Current Release](https://img.shields.io/badge/release-1.5.2-orange.svg?style=flat-square)](https://plugins.jetbrains.com/plugin/8327)
----------------------

<a href="https://discord.gg/j6UNcfr"><img src="https://i.imgur.com/JXu9C1G.png" height="48px"></img></a>

Visit [https://minecraftdev.org](https://minecraftdev.org) for a little information about the project.


Installation
------------

This plugin is available on the [JetBrains IntelliJ plugin repository](https://plugins.jetbrains.com/plugin/8327).

Because of this, you can install the plugin through IntelliJ's internal plugin browser. Navigate to
`File -> Settings -> Plugins` and click the `Browse Repositories...` button at the bottom of the window. In the search
box, simply search for `Minecraft`. You can install it from there and restart IntelliJ to activate the plugin.

Building
--------

JDK 8 is required.

Build the plugin with:

`./gradlew build`

The output .zip file for the plugin will be in `build/distributions`.

Test the plugin in IntelliJ with:

`./gradlew runIde`

Code is generated during the build task, to run the generation task without building use:

`./gradlew generate`

This task is necessary to work on the code without errors before the initial build.

To format the code in this project:

`./gradlew format`

This will format using `ktlint` described below in the [style guide](#style-guide) section below.

The [Gradle IntelliJ Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
will handle downloading the IntelliJ dependencies and packaging the
plugin.

Style Guide
-----------

This projects follows the opinionated [`ktlint`](https://ktlint.github.io/) linter and formatter. It uses the
[`ktlint-gradle`](https://github.com/jlleitschuh/ktlint-gradle) plugin to automatically check and format the code in
this repo.

IDE Setup
---------

It's recommended to run the `ktlintApplyToIdea` and `addKtlintFormatGitPreCommitHook` tasks to configure your
IDE with `ktlint` style settings and to automatically format this project's code before committing:

```
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook
```

IntelliJ includes a lot of dependencies transitively, including common dependencies that are used a lot, such as Kotlin,
Commons Lang3, Guava, etc. Unfortunately, the source distribution for IntelliJ does not contain sources for libraries as
well, so these libraries are imported into the IDE without sources by default. If you want to attach sources for (most)
of the dependencies IntelliJ includes, run the `resolveIntellijLibSources` task and refresh the Gradle project in
IntelliJ:

```
./gradlew resolveIntellijLibSources
```

If you're curious about that task, it is implemented in `buildSrc`.

Developers
----------

- Project Owner - [**@DemonWav** - Kyle Wood](https://github.com/DemonWav)
- [**@Minecrell**](https://github.com/Minecrell)
- [**@PaleoCrafter** - Marvin Rösch](https://github.com/PaleoCrafter)
- [**@RedNesto**](https://github.com/RedNesto)
- [**@Earthcomputer** - Joseph Burton](https://github.com/Earthcomputer)

#### **Significant Contributors**

- [**@gabizou** - Gabriel Harris-Rouquette](https://github.com/gabizou)
- [**@kashike** - Riley Park](https://github.com/kashike)
- [**@jamierocks** - Jamie Mansfield](https://github.com/jamierocks)

License
-------

This project is licensed under [MIT](license.txt).

Supported Platforms
-------------------

- [![Bukkit Icon](src/main/resources/assets/icons/platform/Bukkit.png?raw=true) **Bukkit**](https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse) ([![Spigot Icon](src/main/resources/assets/icons/platform/Spigot.png?raw=true) Spigot](https://spigotmc.org/) and [![Paper Icon](src/main/resources/assets/icons/platform/Paper.png?raw=true) Paper](https://papermc.io/))
- [![Sponge Icon](src/main/resources/assets/icons/platform/Sponge_dark.png?raw=true) **Sponge**](https://www.spongepowered.org/)
- [![Forge Icon](src/main/resources/assets/icons/platform/Forge.png?raw=true) **Minecraft Forge**](https://forums.minecraftforge.net/)
- [![Fabric Icon](src/main/resources/assets/icons/platform/Fabric.png?raw=true) **Fabric**](https://fabricmc.net)
- [![LiteLoader Icon](src/main/resources/assets/icons/platform/LiteLoader.png?raw=true) **LiteLoader**](http://www.liteloader.com/)
- [![MCP Icon](src/main/resources/assets/icons/platform/MCP.png?raw=true) **MCP**](http://www.modcoderpack.com/)
- [![Mixins Icon](src/main/resources/assets/icons/platform/Mixins_dark.png?raw=true) **Mixins**](https://github.com/SpongePowered/Mixin)
- [![BungeeCord Icon](src/main/resources/assets/icons/platform/BungeeCord.png?raw=true) **BungeeCord**](https://www.spigotmc.org/wiki/bungeecord/) ([![Waterfall Icon](src/main/resources/assets/icons/platform/Waterfall.png?raw=true) Waterfall](https://github.com/PaperMC/Waterfall))
