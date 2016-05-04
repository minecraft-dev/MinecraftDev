package com.demonwav.mcdev.platform.sponge;

import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory;
import com.demonwav.mcdev.util.AbstractTemplate;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.Properties;

public class SpongeTemplate extends AbstractTemplate {

    public static void applyMainClassTemplate(Project project, VirtualFile file, String packageName, String className) {
        Properties properties = new Properties();

        properties.setProperty("PACKAGE", packageName);
        properties.setProperty("CLASS_NAME", className);

        try {
            applyTemplate(project, file, MinecraftFileTemplateGroupFactory.SPONGE_MAIN_CLASS_TEMPLATE, properties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
