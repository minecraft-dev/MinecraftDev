/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin;

import com.demonwav.mcdev.MinecraftCodeInsightFixtureTestCase;
import com.demonwav.mcdev.platform.mixin.util.MixinUtils;
import com.demonwav.mcdev.platform.mixin.util.ShadowedMembers;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.Nullable;

public class ShadowTest extends MinecraftCodeInsightFixtureTestCase {

    private static final String path = "src/test/resources/com/demonwav/mcdev/platform/mixin/fixture";
    private PsiClass psiClass;

    @Override
    protected String getTestDataPath() {
        return path;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final PsiFile[] psiFiles = myFixture.configureByFiles("src/test/ShadowData.java", "src/test/MixinBase.java");
        psiClass = ((PsiJavaFile) psiFiles[0]).getClasses()[0];
    }

    private void checkShadow(@Nullable PsiElement element) {
        final ShadowedMembers shadowedElement = MixinUtils.getShadowedElement(element);
        assertNotNull(shadowedElement);
        assertEquals(1, shadowedElement.getTargets().size());
        assertEquals(0, shadowedElement.getErrors().size());
    }

    private void checkBadShadow(@Nullable PsiElement element) {
        final ShadowedMembers shadowedElement = MixinUtils.getShadowedElement(element);
        assertNotNull(shadowedElement);
        assertEquals(1, shadowedElement.getTargets().size());
        assertEquals(1, shadowedElement.getErrors().size());
    }

    private void checkReallyBadShadow(@Nullable PsiElement element) {
        final ShadowedMembers shadowedElement = MixinUtils.getShadowedElement(element);
        assertNotNull(shadowedElement);
        assertEquals(0, shadowedElement.getTargets().size());
        assertEquals(1, shadowedElement.getErrors().size());
    }

    // Good shadows
    public void testPrivateFinalShadow() {
        checkShadow(psiClass.findFieldByName("privateFinalString", false));
    }

    public void testPrivateShadow() {
        checkShadow(psiClass.findFieldByName("privateString", false));
    }

    public void testProtectedFinalShadow() {
        checkShadow(psiClass.findFieldByName("protectedFinalString", false));
    }

    public void testProtectedShadow() {
        checkShadow(psiClass.findFieldByName("protectedString", false));
    }

    public void testPackagePrivateFinalShadow() {
        checkShadow(psiClass.findFieldByName("packagePrivateFinalString", false));
    }

    public void testPackagePrivateShadow() {
        checkShadow(psiClass.findFieldByName("packagePrivateString", false));
    }

    public void testPublicFinalShadow() {
        checkShadow(psiClass.findFieldByName("publicFinalString", false));
    }

    public void testPublicShadow() {
        checkShadow(psiClass.findFieldByName("publicString", false));
    }

    // Bad shadows
    public void testWrongAccessor() {
        checkBadShadow(psiClass.findFieldByName("wrongAccessor", false));
    }

    public void testNoFinal() {
        checkBadShadow(psiClass.findFieldByName("noFinal", false));
    }

    // Really bad shadows
    public void testNonExistent() {
        checkReallyBadShadow(psiClass.findFieldByName("nonExistent", false));
    }
}
