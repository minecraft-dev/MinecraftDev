/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.buildsystem.SourceType;
import com.demonwav.mcdev.facet.MinecraftFacet;
import com.demonwav.mcdev.insight.generation.GenerationData;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.BukkitModule;
import com.demonwav.mcdev.platform.bungeecord.generation.BungeeCordGenerationData;
import com.demonwav.mcdev.platform.bungeecord.util.BungeeCordConstants;
import com.demonwav.mcdev.util.McPsiClass;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import java.util.Arrays;
import javax.swing.Icon;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BungeeCordModule extends AbstractModule {

    private VirtualFile pluginYml;

    BungeeCordModule(@NotNull MinecraftFacet facet) {
        super(facet);
        setup();
    }

    private void setup() {
        pluginYml = getFacet().findFile("plugin.yml", SourceType.RESOURCE);
    }

    @NotNull
    @Override
    public AbstractModuleType<BungeeCordModule> getModuleType() {
        return BungeeCordModuleType.INSTANCE;
    }

    @Nullable
    public VirtualFile getPluginYml() {
        if (pluginYml == null) {
            // try and find the file again if it's not already present
            // when this object was first created it may not have been ready
            setup();
        }
        return pluginYml;
    }

    @NotNull
    @Override
    public PlatformType getType() {
        return PlatformType.BUNGEECORD;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.BUNGEECORD_ICON;
    }

    @Override
    public boolean isEventClassValid(@NotNull PsiClass eventClass, @Nullable PsiMethod method) {
        return BungeeCordConstants.EVENT_CLASS.equals(eventClass.getQualifiedName());
    }

    @NotNull
    @Override
    public String writeErrorMessageForEventParameter(@NotNull PsiClass eventClass, @NotNull PsiMethod method) {
        return "Parameter is not a subclass of net.md_5.bungee.api.plugin.Event\n" +
                "Compiling and running this listener may result in a runtime exception";
    }

    @Override
    public void doPreEventGenerate(@NotNull PsiClass psiClass, @Nullable GenerationData data) {
        final String bungeeCordListenerClass = BungeeCordConstants.LISTENER_CLASS;

        if (!McPsiClass.extendsOrImplements(psiClass, bungeeCordListenerClass)) {
            McPsiClass.addImplements(psiClass, bungeeCordListenerClass);
        }
    }

    @Nullable
    @Override
    public PsiMethod generateEventListenerMethod(@NotNull PsiClass containingClass,
                                                 @NotNull PsiClass chosenClass,
                                                 @NotNull String chosenName,
                                                 @Nullable GenerationData data) {
        PsiMethod method = BukkitModule.generateBukkitStyleEventListenerMethod(
            containingClass,
            chosenClass,
            chosenName,
            getProject(),
            BungeeCordConstants.HANDLER_ANNOTATION,
            false
        );

        final BungeeCordGenerationData generationData = (BungeeCordGenerationData) data;
        if (generationData == null) {
            return method;
        }

        final PsiModifierList modifierList = method.getModifierList();
        final PsiAnnotation annotation = modifierList.findAnnotation(BungeeCordConstants.HANDLER_ANNOTATION);
        if (annotation == null) {
            return method;
        }

        if (generationData.getEventPriority().equals("NORMAL")) {
            return method;
        }

        final PsiAnnotationMemberValue value = JavaPsiFacade.getElementFactory(getProject())
            .createExpressionFromText(BungeeCordConstants.EVENT_PRIORITY_CLASS + "." + generationData.getEventPriority(), annotation);

        annotation.setDeclaredAttributeValue("priority", value);

        return method;
    }

    @Override
    @Contract(value = "null -> false", pure = true)
    public boolean shouldShowPluginIcon(@Nullable PsiElement element) {
        if (!(element instanceof PsiIdentifier)) {
            return false;
        }

        if (!(element.getParent() instanceof PsiClass)) {
            return false;
        }

        final Project project = element.getProject();

        final PsiClass psiClass = (PsiClass) element.getParent();

        final PsiClass pluginClass = JavaPsiFacade.getInstance(project)
            .findClass(BungeeCordConstants.PLUGIN, GlobalSearchScope.allScope(project));

        return pluginClass != null &&
            Arrays.stream(psiClass.getExtendsListTypes())
                .anyMatch(c -> c.equals(PsiTypesUtil.getClassType(pluginClass)));
    }

    @Override
    public void dispose() {
        super.dispose();

        pluginYml = null;
    }
}
