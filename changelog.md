# Minecraft Development for IntelliJ

## [Unreleased]

### Added

- `plugin.yml`, `paper-plugin.yml` and `bungee.yml` main class reference and validity inspection

## [1.8.1] - 2024-08-10

### Added

- Access widener completion in fabric.mod.json
- Event listener generation for Kotlin
- `JUMP` injection point support (without source navigation)
- Inspection highlighting that `JUMP` usages are discouraged
- Inspection highlighting discouraged instruction shifts
- Inspections for when @Inject local capture is unused and for when they can be replaced with @Local
- [#2306](https://github.com/minecraft-dev/MinecraftDev/issues/2306) Use mixin icon for mixin classes
- Documentation while completing keys in mods.toml
- mods.toml support for neoforge.mods.toml
- Automatically insert an `=` after completing a mods.toml key

### Changed

- [#2353](https://github.com/minecraft-dev/MinecraftDev/issues/2353) Move "Method must not be static" error message to static keyword ([#2354](https://github.com/minecraft-dev/MinecraftDev/pull/2354))

### Fixed

- [#2330](https://github.com/minecraft-dev/MinecraftDev/issues/2330) Reformat created files without keeping line breaks. Fixes the Velocity main class annotation's bad formatting.
- [#2331](https://github.com/minecraft-dev/MinecraftDev/issues/2331) Support fabric.mod.json in test resources
- MixinExtras occasional cache desync ([#2335](https://github.com/minecraft-dev/MinecraftDev/pull/2335))
- [#2163](https://github.com/minecraft-dev/MinecraftDev/issues/2163) `@ModifyVariable` method signature checking with `STORE`
- [#2282](https://github.com/minecraft-dev/MinecraftDev/issues/2282) Mixin support confusion with `$` and `.` separators in class names
- Recent NeoModDev version import errors
- Recommended Artifact ID value was not sanitized properly
- NeoForge versions in the Architectury were not being matched correctly for the first version of a major Minecraft release

## [1.8.0] - 2024-07-14

This release contains two major features:
- Support for MixinExtras expressions ([#2274](https://github.com/minecraft-dev/MinecraftDev/pull/2274))
- A rewritten project creator ([#2304](https://github.com/minecraft-dev/MinecraftDev/pull/2304))

### About the new project creator

The new project creator is very similar to the previous one but has a few advantages:
- The templates are now stored on a separate repository and updated the first time you open the creator. This allows us to release template updates independently of plugin releases.
- You can create your own custom templates in their own repositories, which can be:
  - flat directories
  - local ZIP archives
  - remote ZIP archives (like the built-in templates)
- Kotlin templates were added to all platforms except Forge and Architectury (couldn't get the Forge one to work, will look into it later)
- Fabric now has a split sources option
- Some niche options like the plugins dependencies fields were removed as their use was quite limited
- Remembered field values won't be ported over to the new creator, so make sure to configure your Group ID under Build System Properties!
- The old creator will be kept for a few months to give us the time to fix the new creator, please report any issues on the [issue tracker](https://github.com/minecraft-dev/MinecraftDev/issues)

### Added

- Initial support for NeoForge's ModDevGradle
- Option to force json translation and configurable default i18n call ([#2292](https://github.com/minecraft-dev/MinecraftDev/pull/2292))
- Minecraft version detection for Loom-based projects
- Other JVM languages support for translation references, inspections and code folding
- Repo-based project creator templates ([#2304](https://github.com/minecraft-dev/MinecraftDev/pull/2304))
- Support for MixinExtras expressions ([#2274](https://github.com/minecraft-dev/MinecraftDev/pull/2274))

### Changed

- [#2296](https://github.com/minecraft-dev/MinecraftDev/issues/2296) Support entry point container objects in fabric.mod.json
- [#2325](https://github.com/minecraft-dev/MinecraftDev/issues/2325) Make lang annotator fixes bulk compatible
- Migrated the remaining legacy forms to the Kotlin UI DSL

### Fixed

- [#2316](https://github.com/minecraft-dev/MinecraftDev/issues/2316) Sponge's injection inspection isn't aware of the Configurate 4 classes ([#2317](https://github.com/minecraft-dev/MinecraftDev/pull/2317))
- [#2310](https://github.com/minecraft-dev/MinecraftDev/issues/2310) Translations aren't detected for enum constructors
- [#2260](https://github.com/minecraft-dev/MinecraftDev/issues/2260) Vararg return type expected in a @ModifyArg method
