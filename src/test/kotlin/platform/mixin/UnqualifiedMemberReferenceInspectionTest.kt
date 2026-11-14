/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.inspection.reference.UnqualifiedMemberReferenceInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Unqualified Member Reference Inspection Tests")
class UnqualifiedMemberReferenceInspectionTest : BaseMixinTest() {

    private fun doTest(@Language("JAVA") code: String) {
        buildProject {
            dir("test") {
                java("UnqualifiedMemberReferenceMixin.java", code)
            }
        }

        fixture.enableInspections(UnqualifiedMemberReferenceInspection::class)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Unqualified Member Reference")
    fun unqualifiedMemberReference() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.unnecessaryQualifiedMemberReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class UnqualifiedMemberReferenceMixin {

                @Inject(method = "method", at = @At(value = "INVOKE", target = <error descr="Unqualified member reference in @At target">"otherMethod"</error>))
                public void onMethod() {
                }
            }
            """,
        )
    }

    @Test
    @DisplayName("Regex Selector")
    fun regexSelector() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.unnecessaryQualifiedMemberReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class UnqualifiedMemberReferenceMixin {

                @Inject(method = "method", at = @At(value = "INVOKE", target = "/other(Method|Thing)/"))
                public void onMethod() {
                }
            }
            """,
        )
    }
}
