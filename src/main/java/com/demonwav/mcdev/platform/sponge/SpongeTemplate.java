package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.platform.AbstractTemplate;
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.Properties;

public class SpongeTemplate extends AbstractTemplate {

    public static String applyPomTemplate(Module module, String version) {
        Properties properties = new Properties();
        properties.setProperty("BUILD_VERSION", version);

        FileTemplateManager manager = FileTemplateManager.getInstance(module.getProject());
        FileTemplate fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.SPONGE_POM_TEMPLATE);
        try {
            return fileTemplate.getText(properties);
        } catch (IOException e) {
            // TODO what to do when this fails?
            e.printStackTrace();
            return "";
        }
    }

    public static void applyMainClassTemplate(Module module,
                                              VirtualFile mainClassFile,
                                              String packageName,
                                              String className,
                                              SpongeProjectConfiguration configuration) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);
        if (configuration.hasDependencies()) {
            properties.setProperty("HAS_DEPENDENCIES", "true");
        }

        if (configuration.generateDocumentedListeners) {
            properties.setProperty("GENERATE_DOCUMENTATION", "true");
        }

        try {
            applyTemplate(module, mainClassFile, MinecraftFileTemplateGroupFactory.SPONGE_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
