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
import com.demonwav.mcdev.framework.testInspectionFix
import com.demonwav.mcdev.platform.mixin.inspection.injector.InvalidInjectorMethodSignatureInspection
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Invalid Injector Method Signature Inspection Test")
class InvalidInjectorMethodSignatureFixTest : BaseMixinTest() {

    private fun doTest(testName: String) {
        fixture.enableInspections(InvalidInjectorMethodSignatureInspection::class)
        testInspectionFix(fixture, "invalidInjectorMethodSignature/$testName", "Fix method signature")
    }

    @Test
    @DisplayName("Simple case")
    fun simpleCase() = doTest("simpleCase")

    @Test
    @DisplayName("Simple case with MixinExtras Sugar")
    fun simpleCaseWithMixinExtrasSugar() = doTest("simpleCaseWithMixinExtrasSugar")

    @Test
    @DisplayName("With captured locals")
    fun withCapturedLocals() = doTest("withCapturedLocals")

    @Test
    @DisplayName("Simple inner ctor")
    fun simpleInnerCtor() = doTest("simpleInnerCtor")

    @Test
    @DisplayName("Inner ctor with locals")
    fun innerCtorWithLocals() = doTest("innerCtorWithLocals")

    @Test
    @DisplayName("Inject without CallbackInfo")
    fun injectWithoutCI() = doTest("injectWithoutCI")

    @Test
    @DisplayName("ModifyArgs")
    fun modifyArgs() = doTest("modifyArgs")

    @Test
    @DisplayName("Generic method")
    fun genericCase() = doTest("genericCase")

    @Test
    @DisplayName("Generic method complex return type")
    fun genericCaseComplexReturnType() = doTest("genericCaseComplexReturnType")

    @Test
    @DisplayName("Simple method with inner type")
    fun simpleMethodWithInnerType() = doTest("simpleMethodWithInnerType")
}
