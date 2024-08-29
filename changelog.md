# Minecraft Development for IntelliJ

## [Unreleased]

### Added

- [#2391](https://github.com/minecraft-dev/MinecraftDev/issues/2391) Project creator template repo and maven repo authorization
  - this makes it possible to use private GitHub or GitLab repos, or any URL that requires an HTTP `Authorization` or custom header to access
  - it also supports authenticating to Maven repos to list versions when using a `maven_artifact_version` property in your template through basic authentication

### Fixed

- [#2394](https://github.com/minecraft-dev/MinecraftDev/issues/2394) `Write-unsafe context` errors when using the project creator
- [#2406](https://github.com/minecraft-dev/MinecraftDev/issues/2406) `IllegalStateException: Constraint inSmartMode cannot be satisfied` when opening a project
- [#2382](https://github.com/minecraft-dev/MinecraftDev/issues/2382) No Parchment version was selectable when using a version of Minecraft that Parchment doesn't support explicitly
  - In this case, the creator will now select the latest version available for the latest Minecraft version supported by Parchment
- [#2408](https://github.com/minecraft-dev/MinecraftDev/issues/2408) External translation annotations are not attached
  - This happens because IntelliJ IDEA disables external annotations by default in 2024.3, which we rely on for this feature, an opt-out setting has been added to force-enable external annotations in Minecraft projects.
- Cases where references to client sources in fabric.mod.json were not resolved

## [1.8.2] - 2024-10-05

### Added

- `plugin.yml`, `paper-plugin.yml` and `bungee.yml` main class reference and validity inspection
- `mods.toml` and `neoforge.mods.toml` documentation for lookup elements
- Support for split strings within inspections and method target references in Mixins ([#2358](https://github.com/minecraft-dev/MinecraftDev/pull/2358))
- Mouse ungrab on breakpoint hit while running a Gradle task
- JSON5 support to mixin config jsons ([#2375](https://github.com/minecraft-dev/MinecraftDev/pull/2375))
- Value types in mods.toml key completion
- Mixin injection signature fix preview
- Loom 1.8 support
- K2 mode compatibility
- [#2385](https://github.com/minecraft-dev/MinecraftDev/issues/2385) ModDevGradle Vanilla-Mode support

### Changed

- More reliable ClassFqn creator property suggestions and validation
- Lang spellchecking now works in dumb mode

### Fixed

- [#2362](https://github.com/minecraft-dev/MinecraftDev/issues/2362) CME in fabric.mod.json
- Ignored annotations registrations
- NeoGradle and NeoModDev Minecraft version import
- [#2360](https://github.com/minecraft-dev/MinecraftDev/issues/2360) `Class initialization must not depend on services` error
- [#2376](https://github.com/minecraft-dev/MinecraftDev/issues/2376) Error when generating event listeners in read only file
- [#2308](https://github.com/minecraft-dev/MinecraftDev/issues/2308) Mixin Inject signature fix adds last parameter as first local
- [#1813](https://github.com/minecraft-dev/MinecraftDev/issues/1813) Single character Accessor targets aren't inferred correctly
- [#1886](https://github.com/minecraft-dev/MinecraftDev/issues/1886) Sync error in ForgeGradle composite builds

### Changed

- Overhauled Access Transformer support:
  - many lexing errors should now be fixed
  - class names and member names now have their own references, replacing the custom Goto handler
  - SRG names are no longer used on NeoForge 1.20.2+ and a new copy action is available for it
  - the usage inspection no longer incorrectly reports methods overridden in your code or entries covering super methods
  - suppressing inspections is now possible by adding `# Suppress:AtInspectionName` after an entry or at the start of the file, or using the built-in suppress action
  - added an inspection to report unresolved references, to help find out old, superfluous entries
  - added an inspection to report duplicate entries in the same file
  - added formatting support, class and member names are configured to align by default

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
