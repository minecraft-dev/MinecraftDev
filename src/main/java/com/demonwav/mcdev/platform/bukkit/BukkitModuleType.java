package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.insight.generation.ui.EventGenerationPanel;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bukkit.generation.BukkitEventGenerationPanel;
import com.demonwav.mcdev.platform.bukkit.util.BukkitConstants;
import com.demonwav.mcdev.util.CommonColors;

import com.google.common.collect.ImmutableList;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchScopeUtil;
import com.intellij.psi.util.PsiClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;

public class BukkitModuleType extends AbstractModuleType<BukkitModule<?>> {

    private static final String ID = "BUKKIT_MODULE_TYPE";
    private static final BukkitModuleType instance = new BukkitModuleType();

    private BukkitModuleType() {
        super("org.bukkit", "bukkit");
        CommonColors.applyStandardColors(this.colorMap, BukkitConstants.BUKKIT_CHAT_COLOR_CLASS);
    }

    protected BukkitModuleType(final String groupId, final String artifactId) {
        super(groupId, artifactId);

        CommonColors.applyStandardColors(this.colorMap, BukkitConstants.BUKKIT_CHAT_COLOR_CLASS);
    }

    public static BukkitModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.BUKKIT;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.BUKKIT_ICON;
    }

    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return ImmutableList.of(BukkitConstants.BUKKIT_HANDLER_ANNOTATION);
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of(BukkitConstants.BUKKIT_HANDLER_ANNOTATION);
    }

    @NotNull
    @Override
    public BukkitModule generateModule(Module module) {
        return new BukkitModule<>(module, this);
    }
    @Contract(pure = true)
    @Override
    public boolean isEventGenAvailable() {
        return true;
    }

    @NotNull
    @Override
    public EventGenerationPanel getEventGenerationPanel(@NotNull PsiClass chosenClass) {
        return new BukkitEventGenerationPanel(chosenClass);
    }
}
