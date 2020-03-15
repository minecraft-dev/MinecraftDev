/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.sponge.inspection.SpongeInjectionInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Sponge Plugin Class Injection Inspection Tests")
class SpongeInjectionInspectionTest : BaseSpongeTest() {

    private fun doTest(@Language("JAVA") code: String, vararg resourceFiles: String) {
        buildProject {
            src {
                java("test/ASpongePlugin.java", code)
                resourceFiles.forEach { file(it, "", "", true) }
            }
        }

        fixture.enableInspections(SpongeInjectionInspection::class.java)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Primitive Injection Test")
    fun primitiveInjectionTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    private <error descr="Primitive types cannot be injected by Sponge.">int</error> number;
}
""")
    }

    @Test
    @DisplayName("Field Uninjectable Type Type")
    fun uninjectableFieldTypeTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    private <error descr="String cannot be injected by Sponge.">String</error> string;
}
""")
    }

    @Test
    @DisplayName("Constructor Uninjectable Type Test")
    fun constructorUninjectableTypeTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    private String string;

    @Inject
    private ASpongePlugin(<error descr="String cannot be injected by Sponge.">String</error> string) {
        this.string = string;
    }
}
""")
    }

    @Test
    @DisplayName("Constructor Optional Injection Test")
    fun constructorOptionalInjectionTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    private Logger logger;

    @Inject<error descr="Constructor injection cannot be optional.">(optional = true)</error>
    private ASpongePlugin(Logger logger) {
        this.logger = logger;
    }
}
""")
    }

    @Test
    @DisplayName("Method Uninjectable Type Test")
    fun methodUninjectableTypeTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    private String string;

    @Inject
    private void setString(<error descr="String cannot be injected by Sponge.">String</error> string) {
        this.string = string;
    }
}
""")
    }

    @Test
    @DisplayName("Injected Asset Without AssetId Test")
    fun injectedAssetWithoutAssetIdTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    private Asset <error descr="Injected Assets must be annotated with @AssetId.">asset</error>;
}
""")
    }

    @Test
    @DisplayName("Absent Asset Test")
    fun absentAssetTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.asset.AssetId;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    @AssetId(<error descr="Asset 'absent_asset' does not exist.">"absent_asset"</error>)
    private Asset asset;
}
""")
    }

    @Test
    @DisplayName("Asset Is A Directory Test")
    fun assetIsADirectoryTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.asset.AssetId;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    @AssetId(<error descr="AssetId must point to a file.">"dir"</error>)
    private Asset asset;
}
""", "assets/a-plugin/dir/an_asset.txt")
    }

    @Test
    @DisplayName("Path Injection With @ConfigDir and @DefaultConfig Test")
    fun pathInjectionWithConfigDirAndDefaultConfigTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.plugin.Plugin;

import java.io.File;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    @ConfigDir(sharedRoot = false)
    @DefaultConfig(sharedRoot = false)
    private File <error descr="@ConfigDir and @DefaultConfig cannot be used on the same field.">file</error>;
}
""")
    }

    @Test
    @DisplayName("Path Injection Without @ConfigDir Test")
    fun pathInjectionWithoutConfigDirTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.spongepowered.api.plugin.Plugin;

import java.io.File;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    private File <error descr="An injected File must be annotated with either @ConfigDir or @DefaultConfig.">file</error>;
}
""")
    }

    @Test
    @DisplayName("Invalid @DefaultConfig Usage Test")
    fun invalidDefaultConfigUsageTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    <error descr="Logger cannot be annotated with @DefaultConfig.">@DefaultConfig(sharedRoot = false)</error>
    private Logger logger;
}
""")
    }

    @Test
    @DisplayName("@ConfigDir On ConfigurationLoader Test")
    fun configDirOnConfigurationLoaderTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    @DefaultConfig(sharedRoot = false)
    <error descr="Injected ConfigurationLoader cannot be annotated with @ConfigDir.">@ConfigDir(sharedRoot = false)</error>
    private ConfigurationLoader<CommentedConfigurationNode> configurationLoader;
}
""")
    }

    @Test
    @DisplayName("ConfigurationLoader Not Annotated With @DefaultConfig Test")
    fun configurationLoaderNotAnnotatedWithDefaultConfigTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    private ConfigurationLoader<CommentedConfigurationNode> <error descr="Injected ConfigurationLoader must be annotated with @DefaultConfig.">configurationLoader</error>;
}
""")
    }

    @Test
    @DisplayName("ConfigurationLoader Generic Not CommentedConfigurationNode Test")
    fun configurationLoaderGenericNotCommentedConfigurationNodeTest() {
        doTest("""
package test;

import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "a-plugin")
public class ASpongePlugin {
    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<<error descr="Injected ConfigurationLoader generic parameter must be CommentedConfigurationNode.">ConfigurationNode</error>> configurationLoader;
}
""")
    }
}
