/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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
import com.demonwav.mcdev.platform.mixin.inspection.reference.InvalidMemberReferenceInspection
import com.demonwav.mcdev.platform.mixin.inspection.reference.UnresolvedReferenceInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * This is nothing more than a regression test to make sure this doesn't get broken in the future.
 */
@ExtendWith(EdtInterceptor::class)
@DisplayName("Multiple targets test")
class MultipleTargetTest : BaseMixinTest() {

    private fun doTest(@Language("JAVA") code: String) {
        buildProject {
            dir("test") {
                java("AmbiguousReferenceMixin.java", code)
            }
        }

        fixture.enableInspections(UnresolvedReferenceInspection::class, InvalidMemberReferenceInspection::class)
        fixture.checkHighlighting(false, false, false)
    }

    @Test
    @DisplayName("Single Target")
    fun singleTarget() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTarget.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class MultipleTargetMixin {
            
                @Inject(method = "method1", at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """,
        )
    }

    @Test
    @DisplayName("Multiple Targets")
    fun multipleTargets() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTarget.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class MultipleTargetMixin {
            
                @Inject(method = {"method1", "method2"}, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """,
        )
    }
}
