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
import com.demonwav.mcdev.framework.testAllInspectionFixes
import com.demonwav.mcdev.framework.testInspectionFix
import com.demonwav.mcdev.platform.mixin.inspection.injector.InvalidInjectorMethodSignatureInspection
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Invalid Injector Method Signature Inspection Test")
class InvalidInjectorMethodSignatureFixTest : BaseMixinTest() {

    private fun doSingleTest(testName: String) {
        fixture.enableInspections(InvalidInjectorMethodSignatureInspection::class)
        testInspectionFix(fixture, "invalidInjectorMethodSignature/$testName", "Fix method signature")
    }

    private fun doMultiTest(testName: String) {
        fixture.enableInspections(InvalidInjectorMethodSignatureInspection::class)
        testAllInspectionFixes(fixture, "invalidInjectorMethodSignature/$testName", "Fix method signature")
    }

    @Test
    @DisplayName("Inject simple case")
    fun injectSimpleCase() = doSingleTest("inject/simpleCase")

    @Test
    @DisplayName("Inject simple case with MixinExtras Sugar")
    fun injectSimpleCaseWithMixinExtrasSugar() = doSingleTest("inject/simpleCaseWithMixinExtrasSugar")

    @Test
    @DisplayName("Inject with captured locals")
    fun injectWithCapturedLocals() = doSingleTest("inject/withCapturedLocals")

    @Test
    @DisplayName("Inject simple inner ctor")
    fun injectSimpleInnerCtor() = doSingleTest("inject/simpleInnerCtor")

    @Test
    @DisplayName("Inject inner ctor with locals")
    fun injectInnerCtorWithLocals() = doSingleTest("inject/innerCtorWithLocals")

    @Test
    @DisplayName("Inject without CallbackInfo")
    fun injectWithoutCI() = doSingleTest("inject/withoutCI")

    @Test
    @DisplayName("Inject generic method")
    fun injectGenericCase() = doSingleTest("inject/genericCase")

    @Test
    @DisplayName("Inject generic method complex return type")
    fun injectGenericCaseComplexReturnType() = doSingleTest("inject/genericCaseComplexReturnType")

    @Test
    @DisplayName("Inject simple method with inner type")
    fun injectSimpleMethodWithInnerType() = doSingleTest("inject/simpleMethodWithInnerType")

    @Test
    @DisplayName("ModifyArgs simple")
    fun modifyArgsSimple() = doSingleTest("modifyargs/simple")

    @Test
    @DisplayName("ModifyArgs return type only")
    fun modifyArgsReturnTypeOnly() = doSingleTest("modifyargs/returnTypeOnly")

    @Test
    @DisplayName("Inject multi-target")
    fun injectMultiTarget() = doMultiTest("inject/multiTarget")

    @Test
    @DisplayName("ModifyArg")
    fun modifyArg() = doMultiTest("modifyArg")

    @Test
    @DisplayName("ModifyVariable")
    fun modifyVariable() = doMultiTest("modifyVariable")
}
