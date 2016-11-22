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

    private void checkShadow(@Nullable PsiElement element, int targets, int errors) {
        final ShadowedMembers shadowedElement = MixinUtils.getShadowedElement(element);
        assertNotNull(shadowedElement);
        assertEquals(targets, shadowedElement.getTargets().size());
        assertEquals(errors, shadowedElement.getErrors().size());
    }

    private void checkGoodShadow(@Nullable PsiElement element) {
        checkShadow(element, 1, 0);
    }

    private void checkBadShadow(@Nullable PsiElement element) {
        checkShadow(element, 1, 1);
    }

    private void checkReallyBadShadow(@Nullable PsiElement element) {
        checkShadow(element, 0, 1);
    }

    // Good shadows
    public void testPrivateFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("privateFinalString", false));
    }

    public void testPrivateShadow() {
        checkGoodShadow(psiClass.findFieldByName("privateString", false));
    }

    public void testProtectedFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("protectedFinalString", false));
    }

    public void testProtectedShadow() {
        checkGoodShadow(psiClass.findFieldByName("protectedString", false));
    }

    public void testPackagePrivateFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("packagePrivateFinalString", false));
    }

    public void testPackagePrivateShadow() {
        checkGoodShadow(psiClass.findFieldByName("packagePrivateString", false));
    }

    public void testPublicFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("publicFinalString", false));
    }

    public void testPublicShadow() {
        checkGoodShadow(psiClass.findFieldByName("publicString", false));
    }

    // Bad shadows
    public void testWrongAccessor() {
        checkBadShadow(psiClass.findFieldByName("wrongAccessor", false));
    }

    public void testNoFinal() {
        checkBadShadow(psiClass.findFieldByName("noFinal", false));
    }

    public void testTwoIssues() {
        checkShadow(psiClass.findFieldByName("twoIssues", false), 1, 2);
    }

    // Really bad shadows
    public void testNonExistent() {
        checkReallyBadShadow(psiClass.findFieldByName("nonExistent", false));
    }
}
