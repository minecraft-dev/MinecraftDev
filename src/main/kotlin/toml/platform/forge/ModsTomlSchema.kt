/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.toml.platform.forge

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.toml.TomlSchema
import com.intellij.openapi.project.Project
import org.intellij.lang.annotations.Language

object ModsTomlSchema {

    private var cached: TomlSchema? = null

    fun get(project: Project): TomlSchema {
        return cached ?: TomlSchema.parse(project, EXAMPLE_MODS_TOML).also { cached = it }
    }
}

@Language("TOML")
private const val EXAMPLE_MODS_TOML =
    """
# The name of the mod loader type to load - for regular FML @Mod mods it should be javafml.
modLoader="javafml"
# A version range to match for said mod loader - for regular FML @Mod it will be the forge version.
# This is typically bumped every Minecraft version by Forge. See our download page for lists of versions.
loaderVersion="[35,)" 
# The license for you mod. This is mandatory metadata and allows for easier comprehension of your redistributive properties.
# Review your options at <a href="https://choosealicense.com">choosealicense.com</a>. All rights reserved is the default copyright stance, and is thus the default here.
license="All rights reserved"
showAsResourcePack=false
# A URL to refer people to when problems occur with this mod.
issueTrackerURL="http://my.issue.tracker/"
# A list of mods - how many allowed here is determined by the individual mod loader
[[mods]]
# The modid of the mod.
modId="examplemod"
# The version number of the mod - there's a few well known variables usable here or just hardcode it.
# ${ForgeConstants.JAR_VERSION_VAR} will substitute the value of the Implementation-Version as read from the mod's JAR file metadata
version="${ForgeConstants.JAR_VERSION_VAR}"
 # A display name for the mod.
displayName="Example Mod"
# A URL to query for updates for this mod. See the JSON update specification <here>.
updateJSONURL="http://myurl.me/"
# A URL for the "homepage" for this mod, displayed in the mod UI.
displayURL="http://example.com/"
# A file name (in the root of the mod JAR) containing a logo for display.
logoFile="examplemod.png"
logoBlur=false
# A text field displayed in the mod UI.
credits="Thanks for this example mod goes to Java"
# A text field displayed in the mod UI.
authors="Love, Cheese and small house plants"
# Display Test controls the display for your mod in the server connection screen
# MATCH_VERSION means that your mod will cause a red X if the versions on client and server differ. This is the default behaviour and should be what you choose if you have server and client elements to your mod.
# IGNORE_SERVER_VERSION means that your mod will not cause a red X if it's present on the server but not on the client. This is what you should use if you're a server only mod.
# IGNORE_ALL_VERSION means that your mod will not cause a red X if it's present on the client or the server. This is a special case and should only be used if your mod has no server component.
# NONE means that no display test is set on your mod. You need to do this yourself, see IExtensionPoint.DisplayTest for more information. You can define any scheme you wish with this value.
# IMPORTANT NOTE: this is NOT an instruction as to which environments (CLIENT or DEDICATED SERVER) your mod loads on. Your mod should load (and maybe do nothing!) whereever it finds itself.
displayTest="MATCH_VERSION" # MATCH_VERSION is the default if nothing is specified (#optional)

# The description text for the mod (multi line!)
description='''
This is a long form description of the mod. You can write whatever you want here
'''
# A dependency - use the . to indicate dependency for a specific modid. Dependencies are optional.
[[dependencies.examplemod]]
    # the modid of the dependency.
    modId="forge"
    # Does this dependency have to exist - if not, ordering below must be specified.
    mandatory=true
    # The version range of the dependency.
    versionRange="[35,)"
    # An ordering relationship for the dependency - BEFORE or AFTER required if the relationship is not mandatory.
    ordering="NONE"
    # Side this dependency is applied on - BOTH, CLIENT or SERVER.
    side="BOTH"
"""
