/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.inspection.reference.AmbiguousReferenceInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Ambiguous Reference Inspection Tests")
class AmbiguousReferenceInspectionTest : BaseMixinTest() {

    private fun doTest(@Language("JAVA") code: String) {
        buildProject {
            dir("test") {
                java(
                    "MixedIn.java",
                    """
                    package test;

                    class MixedIn {

                        public void method() {
                        }

                        public void method(String string) {
                        }

                        public void uniqueMethod(String string) {
                        }
                    }
                    """,
                    configure = false,
                    allowAst = true
                )
                java("AmbiguousReferenceMixin.java", code)
            }
        }

        fixture.enableInspections(AmbiguousReferenceInspection::class)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Ambiguous Reference")
    fun ambiguousReference() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class AmbiguousReferenceMixin {
            
                @Inject(method = <error descr="Ambiguous reference to method 'method' in target class">"method"</error>, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("No Ambiguous Reference")
    fun noAmbiguousReference() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class AmbiguousReferenceMixin {
            
                @Inject(method = "uniqueMethod", at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("Ambiguous Reference Multiple Targets")
    fun ambiguousReferenceMultipleTargets() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class AmbiguousReferenceMixin {
            
                @Inject(method = {<error descr="Ambiguous reference to method 'method' in target class">"method"</error>, "uniqueMethod"}, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("No Ambiguous Qualified Reference")
    fun noAmbiguousQualifiedReference() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class AmbiguousReferenceMixin {
            
                @Inject(method = "method(Ljava/lang/String;)V", at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("No Ambiguous Reference Multiple Targets")
    fun noAmbiguousReferenceMultipleTargets() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class AmbiguousReferenceMixin {
            
                @Inject(method = {"method(Ljava/lang/String;)V", "uniqueMethod"}, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """
        )
    }
}
