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
import com.demonwav.mcdev.platform.mixin.inspection.shadow.ShadowModifiersInspection
import com.demonwav.mcdev.platform.mixin.inspection.shadow.ShadowTargetInspection
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiJavaFile

class ShadowTest : BaseMinecraftTestCase(MixinModuleType) {

    override fun setUp() {
        super.setUp()

        buildProject<PsiJavaFile> {

            // TODO: Figure out why MixinBase can't be resolved if we use separate files

            java("src/test/MixinBase.java", """
                package test;

                import org.spongepowered.asm.mixin.Shadow;
                import org.spongepowered.asm.mixin.Final;

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

                @org.spongepowered.asm.mixin.Mixin(MixinBase.class)
                class ShadowData {
                    @Shadow @Final private String privateFinalString;
                    @Shadow private String privateString;

                    @Shadow @Final protected String protectedFinalString;
                    @Shadow protected String protectedString;

                    @Shadow @Final String packagePrivateFinalString;
                    @Shadow String packagePrivateString;

                    @Shadow @Final public String publicFinalString;
                    @Shadow public String publicString;

                    <warning descr="Invalid access modifiers, has: public, but target member has: protected">@Shadow public</warning> String wrongAccessor;
                    <warning descr="@Shadow for final member should be annotated as @Final">@Shadow protected</warning> String noFinal;

                    <error descr="Cannot resolve member 'nonExistent' in target class">@Shadow</error> public String nonExistent;

                    <warning descr="@Shadow for final member should be annotated as @Final"><warning descr="Invalid access modifiers, has: protected, but target member has: public">@Shadow protected</warning></warning> String twoIssues;
                }
            """)
        }
    }

    override fun configureModule(module: Module, model: ModifiableRootModel) {
        // If we're lucky, the following code adds the Mixin library to the project
        val mixinPath = FileUtil.toSystemIndependentName(System.getProperty("mixinUrl")!!)

        val project = module.project
        val table = LibraryTablesRegistrar.getInstance().getLibraryTable(project)

        val library = table.createLibrary("Mixin")
        val libraryModel = library.modifiableModel
        libraryModel.addRoot(VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, mixinPath) + JarFileSystem.JAR_SEPARATOR,
            OrderRootType.CLASSES)
        libraryModel.commit()
        model.addLibraryEntry(library)
    }

    // TODO: Split up in separate tests
    fun test() {
        myFixture.enableInspections(ShadowTargetInspection::class.java, ShadowModifiersInspection::class.java)
        myFixture.checkHighlighting(true, false, false)
    }


}
