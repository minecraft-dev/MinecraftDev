# Minecraft Development for IntelliJ

## [1.8.21]

### Fixed

- Fixed decompile button not working for Fabric and VanillaGradle projects
- Support debugging interpolated MixinExtras expression strings
- Fixed shadow warning on abstract enum constructors

## [1.8.20]

### Added

- Added reference provider and auto-complete for `@Mixin(target)`

### Changed

- Hide the Yarn version selector for Fabric 26.1+ project creation

### Fixed

- Fixed empty template repo list issues

## [1.8.19]

### Fixed

- NeoForged ModDev Gradle sync failing on IntelliJ 2026.2

## [1.8.18]

### Fixed

- Write-unsafe context errors during project creation

## [1.8.17]

### Added

- Added support for IntelliJ 2026.2
- Added completion contributor for Bukkit event handlers

## [1.8.16]

### Added

- Added autocompletion for `@Desc` selector

### Fixed

- Fixed freeze of analysis in occasional classes caused by `TranslationIdentifier`

## [1.8.15]

### Added

- Added an error for unrecognized class tweaker versions.
- Added support for project template version ranges, meaning MinecraftDev can now once again load v1 and v2 project templates.
  - Improved the error message for invalid template versions.
- Inspection for missing abstract shadows in an enum mixin, for when the target enum has abstract methods.
- Added a quick fix for missing constructor in enum mixin to add a shadow constructor.
- Abstract shadows in enum mixins are no longer reported as unused.

### Fixes

- Fixed token errors in class tweaker files until file is reopened.
- Fixed autocompletion for class tweaker headers.
  - Now includes the `official` namespace, and only the namespaces available for the project.
  - Now includes `classTweaker v2`.
- `@Shadow` methods for static methods no longer generate as abstract.
- `@Shadow` methods for methods in an enum mixin no longer generate as abstract.
- The "Mixin instantiated within that same mixin using a constructor which doesn't exist" inspection no longer reports when the constructor is unresolved (generates a compile error).

## [1.8.14]

### Added

- Enhancements to enum mixins:
  - Warning for added enum constants not prefixed by the mod id
  - Suppress unused warnings for added enum constants
  - Line marker for added enum constants
  - Error for when enum mixins target non-enum classes
  - Error for when a switch statement or expression is used on an enum mixin
  - Error for when a constructor is used that doesn't exist in the target class

## [1.8.13]

### Added

- Added support for Mixin-based enum extensions.
- Added support for ClassTweaker v2

### Fixed

- Fixed Paper and Velocity templates
- Fixed `No signature of method: java.lang.String.getOrNull() is applicable for argument types: () values: []` when importing a NeoForm project
- Fixed a possible deadlock when detecting Minecraft frameworks

## [1.8.12]

### Version Support
- Added IntelliJ 2026.1
- Dropped IntelliJ 2025.1

### Added

- Automatic fetching of Paper versions in the project wizard.
- More advanced customization in custom project templates, including:
  - Visibility control for property groups.
  - Inline Gradle plugin enabling and version selection.
- Several new inspections for mixins:
  - `@Shadow` parameter names not matching their target.
  - Parameter name mismatch compared to the target class in unobfuscated versions.
  - Mixin class types that don't match the target's type.

### Changed

- The Run and Build tool windows now open automatically during project creation or Maven import.
- Updated Fabric mixin parameter handling for versions 0.17.0 and higher.
- Downgraded the parameter name mismatch inspection to a weak warning.

### Fixed

- Fixed translation folding when arguments are provided as an explicit array.
- Fixed an issue where the Paper library would sometimes not be detected.
- Fixed unintended renaming of mixin targets incorrectly affecting strings and non-Java files.
- Fixed local and inner classes being incorrectly disallowed in mixins.
- Improved the accuracy of parameter name inspections by correctly handling local variable table (LVT) ordering.
- Fixed NeoForge version detection when in vanilla mode.

## [1.8.11]

### Added

- Using Minecraft version to recognize unobfuscated Minecraft

### Fixed

- Downgraded non-void WrapWithCondition to warning if there is a pop instruction afterwards
- Fixed local templates freezing
- Avoided a crash for unknown MC versions
- Don't suggest that locals can use names with argsOnly = true

## [1.8.10]

### Fixed

- Fixed `transitive-*` being reported as an error in class tweakers.
- Fixed all local classes being reported as being inside a mixin.
- Fixed write-unsafe context errors when using the project creator.

## [1.8.9]

### Fixed

- Fixed freeze in project creator
- Fixed ModifyExpressionValue sometimes being detected as disallowed if MixinExtras expressions are used

## [1.8.8]

### Fixed

- `@At` targets inside `@Slice` are no longer checked for invalid instructions according to the injector type.

## [1.8.7]

### Added

- Translations inside `deprecated.json` are now properly handled.
  - Usages of removed or renamed translations will be reported as a warning.
  - Translations will now properly be redirected to the correct entry in the translation file if they have been renamed.
- New error if a local class is used in a mixin.
- Access wideners have been renamed to class tweakers, which are a new beta Fabric feature, backwards compatible with
  access wideners. Both the new class tweaker format and the legacy access widener format are supported.
- Added warnings related to `@At.opcode`:
  - A warning if it is used on any injection point other than `FIELD` (it would be ignored).
  - A warning if it is *not* used on `FIELD`.
  - A warning if an unsupported opcode is used.
- Added an inspection for when an injector targets an unsupported instruction. This includes:
  - `@Redirect` and `@WrapOperation` targeting any instruction except those that can be targeted according to their docs.
  - `@ModifyArg` and `@ModifyArgs` targeting any instruction except method invocations.
  - `@ModifyConstant` targeting any instruction that doesn't push a constant.
  - `@ModifyExpressionValue` targeting instructions that don't return a value (e.g. void method invocations).
  - `@ModifyReceiver` targeting any instruction except non-static method invocations and field references.
  - `@ModifyReturnValue` targeting any instruction except `xRETURN`.
  - `@WrapWithCondition` targeting any instruction except void method invocations and field assignments.
- Added an inspection for when `@Share` is used on a parameter that isn't from the `LocalRef` family.
- Added an inspection for when two `@Share` annotations pointing to the same variable have a different type.
- Added support for target code which has named variables in the LVT. This is enabled by default on all code except in
  the packages `net.minecraft` and `com.mojang.blaze3d`. It can be enabled for these packages too by enabling the
  `mcdev.unobfuscated.minecraft` registry value in the registry menu (access via ctrl+shift+A in the IDE and search for
  "Registry"). Plugins can add further unobfuscated packages by registering to the
  `com.demonwav.minecraft-dev.missingLVTChecker` extension point.
  - `@Local` and `@ModifyVariable` will generate warnings if the target class has named variables in the LVT, but are
    being targeted with `ordinal`, `index`, or in implicit mode.
    - This is currently disabled for parameter locals (those that can have `argsOnly = true`, due to a
      [Mixin issue](https://github.com/FabricMC/Mixin/issues/189)).
    - There is a quick fix to convert the annotation to use `name`.
  - MixinExtras expressions will now auto-complete to use named `@Local` targets where possible.
  - The `@Inject` local capture migration quick fix now also uses named `@Local` targets where possible.

### Changed

- Simplified translation identification code and made interpolation inlining less aggressive. When folding translations,
  MinecraftDev will no longer attempt to inline the values of variables in the substitution.
- Removed the check for injecting before a `super()` call. This was never a correct thing to check for.
- Switched from using entry points to implicit usage providers in mixins. This allows MixinExtras sugar parameters to be
  reported as unused by IntelliJ if they are unused, while maintaining an implicit usage on the whole method to avoid an
  unused warning there.
- Changed `@Local` and `@ModifyVariable` can be `argsOnly = true` inspection to also work if the local is specified by
  name.

### Fixed

- Error reporting now works again and uses Sentry. If you have had problems reporting errors to MinecraftDev, it may be
  worth trying again after this update.
- Fixed auto-completion for `NEW` and `FIELD` targets so they now read from the bytecode rather than the source code,
  improving accuracy.
- Injection point specifiers and ordinals no longer prevent completion of `@At` targets.
- `@Local` no longer always reports as able to be `argsOnly = true` if there are other annotations on the method, which
  was particularly noticeable when using `@Expression`.
- Fixed `@ModifyConstant`'s expected method signature for class types. It now correctly expects two arguments, an
  `Object` instance and a `Class<?>` type, and may return a `Class<?>` or a `boolean`.
- MinecraftDev no longer uses the internal `loom.mods[...].modSourceSets` property when importing a Fabric project.
- Double nested anonymous class resolving now works, so mixins targeting `Foo$1$1` should now work correctly.
- Fixed navigation to constructors from mixins, which was broken in the previous release.
- Class tweaker (access widener) files no longer treat unrecognized namespaces as a syntax error.
  - A warning will be generated if the namespace is not in the current mapping file (pulled from Loom). If the current
    mapping file does not exist, only `official` will be recognized as a valid namespace.
- Fixed the "copy AT entry" action for unobfuscated environments.
- Fixed an `ArrayIndexOutOfBoundsException` in `LocalVariables.guessAllLocalVariablesUncached`.
- Override `getLanguage` in `NbttCodeStyleSettingsProvider`, preventing an IDE error.
- Weaken the assertions in `TranslationFiles`, showing a balloon to the user instead. This prevents an IDE error from
  occurring.

## [1.8.6]

### Added

- Debug flow diagrams for MixinExtras expressions.
- An inspection to replace `INVOKE_ASSIGN` with MixinExtras expressions where possible.
  - Expressions are preferred because `INVOKE_ASSIGN` doesn't fail if there's no assignment.

### Changed

- Performance improvements for shadow completions.
- Smarter warnings for discouraged shifting
  - Shifting is now always discouraged on injectors that can't inject on any instruction in the method (such as `@Redirect`).
  - When MixinExtras expressions are being used, the discourage shifting logic now takes into account the type of expression.
- Mixin navigation to lambdas has been improved. MinecraftDev will now properly detect that a synthetic method will be
  generated for method references in the following additional cases:
  - All array method references.
  - Method references to a superclass method (`super::foo`).
  - Constructor method references for a non-static inner class.
    - Previously, all constructor method references were incorrectly assumed to generate a synthetic method.
  - Varargs parameter expansions.
  - Access to private methods in an outer or inner class before Java 9.
  - Access to protected methods in the superclass of an outer class, if the superclass is in a different package.
  - Cases where the functional interface method being implemented has a parameter which is an intersection type (a type
    parameter extending multiple things).

### Fixed

- Fixed "generate accessor/invoker" action not doing anything.
- Fixed Mixin version detection in legacy/non-standard Minecraft environments. The Mixin version is now taken from the
  `MixinBootstrap` class, rather than attempting to decipher it from the library artifact.
- Fixed `@Coerce` on return type wanting subtypes not supertypes.
- Fixed the "add definition" quick-fix for MixinExtras expressions creating a "dummy" definition since 2025.1
- Fixed incorrect unused warnings on MixinExtras expression definitions
- Fixed "find usages" on MixinExtras expression definitions
- Fixed IDE error that sometimes occurs when navigating to mixin target
- Fixed mixin navigation to field assignments.

## [1.8.5]

### Added

- Support injection point specifiers everywhere
- Support for the 2025.2 EAP

### Changed

- Demote a couple of mixin inspections from error to warning
- Allow handler methods to be static even when not required, except for `@Inject` on stock mixin
- Allow `CallbackInfoReturnable.cancel()` in MixinCancellableInspection
- Check bases for `CIR.cancel()`
- Allow `.aw` as alternative access widener file extension
- Remove shadow completions explicit proximity

### Fixed

- [#2148](https://github.com/minecraft-dev/MinecraftDev/issues/2148) Allow assignment to `@Final` fields in class initializer blocks
- [#2468](https://github.com/minecraft-dev/MinecraftDev/issues/2468) entrypoint checks on non-ending parts of entrypoint references, and handle escapes in entrypoint reference strings
- [#2470](https://github.com/minecraft-dev/MinecraftDev/issues/2470) Various Mixin fixes
- `@Shadow` suggestions being incorrectly prioritized
- `NoSuchElementException` in MixinAnnotationTargetInspection
- `IllegalStateException` when typing a colon in Java code

## [1.8.4]

### Added

- Added datagen to Fabric project templates

## [1.8.3]

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
