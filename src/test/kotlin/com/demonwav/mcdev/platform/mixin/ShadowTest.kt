/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.BaseMinecraftTestCase
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import org.junit.Assert

class ShadowTest : BaseMinecraftTestCase(MixinModuleType) {

    private lateinit var psiClass: PsiClass

    override fun setUp() {
        super.setUp()
        psiClass = buildProject<PsiJavaFile> {
            java("src/test/ShadowData.java", """
                package test;

                @org.spongepowered.asm.mixin.Mixin("test.MixinBase")
                public class ShadowData {
                    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private String privateFinalString;
                    @org.spongepowered.asm.mixin.Shadow private String privateString;

                    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final protected String protectedFinalString;
                    @org.spongepowered.asm.mixin.Shadow protected String protectedString;

                    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final String packagePrivateFinalString;
                    @org.spongepowered.asm.mixin.Shadow String packagePrivateString;

                    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final public String publicFinalString;
                    @org.spongepowered.asm.mixin.Shadow public String publicString;

                    @org.spongepowered.asm.mixin.Shadow public String wrongAccessor;
                    @org.spongepowered.asm.mixin.Shadow protected String noFinal;

                    @org.spongepowered.asm.mixin.Shadow public String nonExistent;

                    @org.spongepowered.asm.mixin.Shadow protected String twoIssues;
                }
            """)

            java("src/test/MixinBase.java", """
                package test;

                public class MixinBase {
                    // Static
                    private static final String privateStaticFinalString = "";
                    private static String privateStaticString = "";

                    protected static final String protectedStaticFinalString = "";
                    protected static String protectedStaticString = "";

                    static final String packagePrivateStaticFinalString = "";
                    static String packagePrivateStaticString = "";

                    public static final String publicStaticFinalString = "";
                    public static String publicStaticString = "";

                    // Non-static
                    private final String privateFinalString = "";
                    private String privateString = "";

                    protected final String protectedFinalString = "";
                    protected String protectedString = "";

                    final String packagePrivateFinalString = "";
                    String packagePrivateString = "";

                    public final String publicFinalString = "";
                    public String publicString = "";

                    // Bad shadows
                    protected String wrongAccessor = "";
                    protected final String noFinal = "";

                    public final String twoIssues = "";
                }
            """)
        }[0].classes.single()
    }

    private fun checkShadow(element: PsiElement?, targets: Int, errors: Int) {
        val shadowedElement = MixinUtils.getShadowedElement(element)
        Assert.assertNotNull(shadowedElement)
        Assert.assertEquals(targets, shadowedElement.targets.size)
        Assert.assertEquals(errors, shadowedElement.errors.size)
    }

    private fun checkGoodShadow(element: PsiElement?) {
        checkShadow(element, 1, 0)
    }

    private fun checkBadShadow(element: PsiElement?) {
        checkShadow(element, 1, 1)
    }

    private fun checkReallyBadShadow(element: PsiElement?) {
        checkShadow(element, 0, 1)
    }

    // Good shadows
    fun testPrivateFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("privateFinalString", false))
    }

    fun testPrivateShadow() {
        checkGoodShadow(psiClass.findFieldByName("privateString", false))
    }

    fun testProtectedFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("protectedFinalString", false))
    }

    fun testProtectedShadow() {
        checkGoodShadow(psiClass.findFieldByName("protectedString", false))
    }

    fun testPackagePrivateFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("packagePrivateFinalString", false))
    }

    fun testPackagePrivateShadow() {
        checkGoodShadow(psiClass.findFieldByName("packagePrivateString", false))
    }

    fun testPublicFinalShadow() {
        checkGoodShadow(psiClass.findFieldByName("publicFinalString", false))
    }

    fun testPublicShadow() {
        checkGoodShadow(psiClass.findFieldByName("publicString", false))
    }

    // Bad shadows
    fun testWrongAccessor() {
        checkBadShadow(psiClass.findFieldByName("wrongAccessor", false))
    }

    fun testNoFinal() {
        checkBadShadow(psiClass.findFieldByName("noFinal", false))
    }

    fun testTwoIssues() {
        checkShadow(psiClass.findFieldByName("twoIssues", false), 1, 2)
    }

    // Really bad shadows
    fun testNonExistent() {
        checkReallyBadShadow(psiClass.findFieldByName("nonExistent", false))
    }
}
