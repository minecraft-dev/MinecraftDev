/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

/*
 * This file contains substantial amounts of code from Mixin, licensed under the MIT License (MIT).
 * See https://github.com/SpongePowered/Mixin/blob/master/src/main/java/org/spongepowered/asm/util/Locals.java
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.psiType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiStatement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import com.intellij.psi.controlFlow.ControlFlow
import com.intellij.psi.controlFlow.ControlFlowFactory
import com.intellij.psi.controlFlow.ControlFlowInstructionVisitor
import com.intellij.psi.controlFlow.ControlFlowOptions
import com.intellij.psi.controlFlow.Instruction
import com.intellij.psi.controlFlow.LocalsControlFlowPolicy
import com.intellij.psi.controlFlow.WriteVariableInstruction
import com.intellij.psi.scope.util.PsiScopesUtil
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import kotlin.math.min
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.BasicValue

object LocalVariables {
    private val LOCAL_INDEX_KEY = Key<Int>("mcdev.local_index")

    /**
     * Guesses the local variable index of the given variable, or of implicit locals at the given element.
     * Only valid after [guessLocalsAt] has been called.
     */
    fun guessLocalVariableIndex(element: PsiElement): Int? {
        return element.getUserData(LOCAL_INDEX_KEY)
    }

    fun guessLocalsAt(element: PsiElement, argsOnly: Boolean, start: Boolean): List<SourceLocalVariable> {
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, PsiLambdaExpression::class.java)
            ?: return emptyList()
        val actualMethod = method.parentOfType<PsiMethod>(withSelf = true) ?: return emptyList()
        val args = mutableListOf<SourceLocalVariable>()

        var argsIndex = 0
        if (!actualMethod.hasModifierProperty(PsiModifier.STATIC)) {
            args += SourceLocalVariable("this", actualMethod.containingClass?.psiType ?: return emptyList(), 0)
            argsIndex++
        }

        for (parameter in method.parameterList.parameters) {
            val mixinName = if (argsOnly) "var$argsIndex" else parameter.name
            args += SourceLocalVariable(parameter.name, parameter.type, argsIndex, mixinName = mixinName)
            argsIndex++
            if (parameter.isDoubleSlot) {
                argsIndex++
            }
        }

        if (argsOnly) {
            return args
        }

        val body = method.body ?: return args
        val controlFlow = ControlFlowFactory.getControlFlow(
            body,
            LocalsControlFlowPolicy(body),
            ControlFlowOptions.NO_CONST_EVALUATE
        )

        val allLocalVariables = guessAllLocalVariables(argsIndex, body, controlFlow)
        val elementOffset = if (start) controlFlow.getStartOffset(element) else controlFlow.getEndOffset(element)
        return args + (allLocalVariables.getOrNull(elementOffset) ?: emptyList())
    }

    private fun guessAllLocalVariables(
        argsSize: Int,
        body: PsiElement,
        controlFlow: ControlFlow
    ): Array<List<SourceLocalVariable>> {
        return body.cached(PsiModificationTracker.MODIFICATION_COUNT) {
            guessAllLocalVariablesUncached(argsSize, body, controlFlow)
        }
    }

    private fun guessAllLocalVariablesUncached(
        argsSize: Int,
        body: PsiElement,
        controlFlow: ControlFlow
    ): Array<List<SourceLocalVariable>> {
        val method = body.parent
        val allLocalVariables = getAllLocalVariables(body)
        for (variable in allLocalVariables) {
            var localIndex = argsSize
            // gets all local variable declarations in scope at the declaration of variable
            PsiScopesUtil.treeWalkUp(
                { elem, _ ->
                    localIndex += getLocalVariableSize(elem)
                    true
                },
                variable,
                method
            )
            // add on other implicit declarations in scope
            for (parent in generateSequence(variable.parent, PsiElement::getParent).takeWhile { it != method }) {
                localIndex += getLocalVariableSize(parent)
            }
            variable.putUserData(LOCAL_INDEX_KEY, localIndex)
        }

        // take into account implicit locals for certain constructs (e.g. foreach loops)
        val extraVariables = mutableMapOf<Int, MutableList<SourceLocalVariable>>()
        for (variable in allLocalVariables) {
            val extraVars = when (variable) {
                is PsiVariable -> continue
                is PsiForeachStatement -> variable.getExtraLocals()
                else -> continue
            }
            val enclosingStatement = variable.parentOfType<PsiStatement>(withSelf = true) ?: continue
            extraVariables.getOrPut(controlFlow.getStartOffset(enclosingStatement)) { mutableListOf() } += extraVars
        }

        // compute the local variables that are definitely initialized and not overwritten at each offset
        class MyVisitor : ControlFlowInstructionVisitor() {
            val locals = arrayOfNulls<Array<SourceLocalVariable?>>(controlFlow.size + 1)
            val instructionQueue = ArrayDeque<Int>()

            override fun visitWriteVariableInstruction(
                instruction: WriteVariableInstruction,
                offset: Int,
                nextOffset: Int
            ) {
                if (instruction.variable in allLocalVariables) {
                    val localIndex = instruction.variable.getUserData(LOCAL_INDEX_KEY)!!
                    var localsHere = this.locals[offset]
                        ?: arrayOfNulls<SourceLocalVariable>(localIndex + 1).also { this.locals[offset] = it }
                    if (localIndex >= localsHere.size) {
                        localsHere = localsHere.copyOf(localIndex + 1)
                    }
                    val name = instruction.variable.name ?: return
                    localsHere[localIndex] = SourceLocalVariable(name, instruction.variable.type, localIndex)
                    if (instruction.variable.isDoubleSlot && localIndex + 1 < localsHere.size) {
                        localsHere[localIndex + 1] = null
                    }
                    this.locals[offset] = localsHere
                }
                visitInstruction(instruction, offset, nextOffset)
            }

            override fun visitInstruction(instruction: Instruction, offset: Int, nextOffset: Int) {
                val extraVars = extraVariables[offset]
                if (extraVars != null) {
                    for (variable in extraVars) {
                        val localsHere = this.locals[offset]
                            ?: arrayOfNulls<SourceLocalVariable>(variable.index + 1).also { this.locals[offset] = it }
                        localsHere[variable.index] = variable
                        if (variable.type == PsiType.LONG || variable.type == PsiType.DOUBLE) {
                            if (variable.index + 1 < localsHere.size) {
                                localsHere[variable.index + 1] = null
                            }
                        }
                    }
                }
                for (i in 0 until instruction.nNext()) {
                    visitEdge(offset, instruction.getNext(offset, i))
                }
            }

            private fun visitEdge(offset: Int, nextOffset: Int) {
                val localsHere = this.locals[offset] ?: emptyArray()
                var changed = false
                val nextLocals = this.locals[nextOffset]
                if (nextLocals == null) {
                    this.locals[nextOffset] = localsHere.clone()
                    changed = true
                } else {
                    for (i in localsHere.size until nextLocals.size) {
                        if (nextLocals[i] != null) {
                            nextLocals[i] = null
                            changed = true
                        }
                    }
                    for (i in 0 until min(localsHere.size, nextLocals.size)) {
                        if (nextLocals[i] != localsHere[i]) {
                            if (nextLocals[i] != null) {
                                nextLocals[i] = null
                                changed = true
                            }
                        }
                    }
                }
                if (changed) {
                    instructionQueue.add(nextOffset)
                }
            }
        }

        // walk the control flow graph
        val visitor = MyVisitor()
        visitor.instructionQueue.add(0)
        while (visitor.instructionQueue.isNotEmpty()) {
            val offset = visitor.instructionQueue.removeFirst()
            val insn = controlFlow.instructions.getOrNull(offset) ?: continue
            insn.accept(visitor, offset, offset + 1)
        }

        return visitor.locals.mapToArray { it?.filterNotNull() ?: emptyList() }
    }

    private fun getAllLocalVariables(body: PsiElement): List<PsiElement> {
        val locals = mutableListOf<PsiElement>()
        body.accept(
            object : JavaRecursiveElementVisitor() {
                override fun visitVariable(variable: PsiVariable) {
                    locals += variable
                    super.visitVariable(variable)
                }

                override fun visitForeachStatement(statement: PsiForeachStatement) {
                    locals += statement
                    super.visitForeachStatement(statement)
                }

                override fun visitClass(aClass: PsiClass?) {
                    // don't recurse into classes
                }

                override fun visitMethod(method: PsiMethod?) {
                    // don't recurse into methods
                }

                override fun visitLambdaExpression(expression: PsiLambdaExpression?) {
                    // don't recurse into lambdas
                }
            }
        )
        return locals
    }

    fun getLocalVariableSize(element: PsiElement): Int {
        return when (element) {
            // longs and doubles take two slots
            is PsiVariable -> if (element.isDoubleSlot) 2 else 1
            // arrays have copy of array, length and index variables, iterables have the iterator variable
            is PsiForeachStatement -> if (element.iterationParameter.type is PsiArrayType) 3 else 1
            else -> 0
        }
    }

    private val PsiVariable.isDoubleSlot: Boolean
        get() = type == PsiType.DOUBLE || type == PsiType.LONG

    private fun PsiForeachStatement.getExtraLocals(): List<SourceLocalVariable> {
        val localIndex = getUserData(LOCAL_INDEX_KEY)!!
        val iterable = iteratedValue ?: return emptyList()
        val type = iterable.type
        if (type is PsiArrayType) {
            return listOf(
                // array
                SourceLocalVariable(
                    "var$localIndex",
                    type,
                    localIndex,
                    implicitLoadCountBefore = 1,
                    implicitStoreCountBefore = 1
                ),
                // length
                SourceLocalVariable(
                    "var${localIndex + 1}",
                    PsiType.INT,
                    localIndex + 1,
                    implicitStoreCountBefore = 1,
                    implicitLoadCountAfter = 1
                ),
                // index
                SourceLocalVariable(
                    "var${localIndex + 2}",
                    PsiType.INT,
                    localIndex + 2,
                    implicitStoreCountBefore = 1,
                    implicitLoadCountBefore = 1,
                    implicitLoadCountAfter = 1
                )
            )
        } else {
            val iteratorType = JavaPsiFacade.getElementFactory(project)
                .createTypeByFQClassName(
                    CommonClassNames.JAVA_UTIL_ITERATOR,
                    resolveScope
                )
            return listOf(
                // iterator
                SourceLocalVariable(
                    "var$localIndex",
                    iteratorType,
                    localIndex,
                    implicitStoreCountBefore = 1,
                    implicitLoadCountBefore = 1
                )
            )
        }
    }

    fun getLocals(
        module: Module,
        classNode: ClassNode,
        method: MethodNode,
        node: AbstractInsnNode
    ): Array<LocalVariable?>? {
        return getLocals(module.project, classNode, method, node, detectCurrentSettings(module))
    }

    private fun getLocals(
        project: Project,
        classNode: ClassNode,
        method: MethodNode,
        nodeArg: AbstractInsnNode,
        settings: Settings
    ): Array<LocalVariable?>? {
        return try {
            doGetLocals(project, classNode, method, nodeArg, settings)
        } catch (e: LocalAnalysisFailedException) {
            null
        }
    }

    private val resurrectLocalsChange = SemanticVersion.release(0, 8, 3)
    private fun detectCurrentSettings(module: Module): Settings {
        val mixinVersion = MinecraftFacet.getInstance(module, MixinModuleType)?.mixinVersion
            ?: throw LocalAnalysisFailedException()
        return if (mixinVersion < resurrectLocalsChange) {
            Settings.NO_RESURRECT
        } else {
            Settings.DEFAULT
        }
    }

    private fun doGetLocals(
        project: Project,
        classNode: ClassNode,
        method: MethodNode,
        nodeArg: AbstractInsnNode,
        settings: Settings
    ): Array<LocalVariable?> {
        var node = nodeArg
        for (i in 0 until 3) {
            if (node !is LabelNode && node !is LineNumberNode) {
                break
            }
            val nextNode = method.instructions.nextNode(node)
            if (nextNode is FrameNode) { // Do not ffwd over frames
                break
            }
            node = nextNode
        }

        val frames = method.instructions.iterator().asSequence().filterIsInstance<FrameNode>().toList()
        val frame = arrayOfNulls<LocalVariable>(method.maxLocals)
        var local = 0
        var index = 0

        // Initialise implicit "this" reference in non-static methods
        if (!method.hasAccess(Opcodes.ACC_STATIC)) {
            frame[local++] = LocalVariable("this", Type.getObjectType(classNode.name).toString(), null, null, null, 0)
        }

        // Initialise method arguments
        for (argType in Type.getArgumentTypes(method.desc)) {
            frame[local] = LocalVariable("arg" + index++, argType.toString(), null, null, null, local)
            local += argType.size
        }

        val initialFrameSize = local
        var frameSize = local
        var frameIndex = -1
        var lastFrameSize = local
        var knownFrameSize = local
        var storeInsn: VarInsnNode? = null

        for (insn in method.instructions) {
            // Tick the zombies
            for (zombie in frame.asSequence().filterIsInstance<ZombieLocalVariable>()) {
                zombie.lifetime++
                if (insn is FrameNode) {
                    zombie.frames++
                }
            }

            if (storeInsn != null) {
                val storedLocal = getLocalVariableAt(project, classNode, method, insn, storeInsn.`var`)
                frame[storeInsn.`var`] = storedLocal
                knownFrameSize = knownFrameSize.coerceAtLeast(storeInsn.`var` + 1)
                if (storedLocal != null &&
                    storeInsn.`var` < method.maxLocals - 1 &&
                    storedLocal.desc != null &&
                    Type.getType(storedLocal.desc).size == 2
                ) {
                    frame[storeInsn.`var` + 1] = null // TOP
                    knownFrameSize = knownFrameSize.coerceAtLeast(storeInsn.`var` + 2)
                    if (settings.resurrectExposedOnStore) {
                        resurrect(frame, knownFrameSize, settings)
                    }
                }
                storeInsn = null
            }

            if (insn is FrameNode) {
                fun handleFrame() {
                    frameIndex++
                    if (insn.type == Opcodes.F_SAME || insn.type == Opcodes.F_SAME1) {
                        return
                    }
                    val frameNodeSize = insn.computeFrameSize(initialFrameSize)
                    val frameData = frames.getOrNull(frameIndex)
                    if (frameData != null) {
                        if (frameData.type == Opcodes.F_FULL) {
                            frameSize = frameNodeSize.coerceAtLeast(initialFrameSize)
                            lastFrameSize = frameSize
                            knownFrameSize = lastFrameSize
                        } else {
                            frameSize = getAdjustedFrameSize(
                                frameSize,
                                frameData.type,
                                frameData.computeFrameSize(initialFrameSize),
                                initialFrameSize
                            )
                        }
                    } else {
                        frameSize =
                            getAdjustedFrameSize(
                                frameSize,
                                insn.type,
                                frameNodeSize,
                                initialFrameSize
                            )
                    }

                    // Sanity check
                    if (frameSize < initialFrameSize) {
                        throw IllegalStateException(
                            "Locals entered an invalid state evaluating " +
                                "${classNode.name}::${method.name}${method.desc} at instruction " +
                                "${method.instructions.indexOf(insn)}. Initial frame size is" +
                                " $initialFrameSize, calculated a frame size of $frameSize"
                        )
                    }
                    if ((
                        (frameData == null && (insn.type == Opcodes.F_CHOP || insn.type == Opcodes.F_NEW)) ||
                            (frameData != null && frameData.type == Opcodes.F_CHOP)
                        )
                    ) {
                        for (framePos in frameSize until frame.size) {
                            frame[framePos] = ZombieLocalVariable.of(frame[framePos], ZombieLocalVariable.CHOP)
                        }
                        lastFrameSize = frameSize
                        knownFrameSize = lastFrameSize
                        return
                    }
                    var framePos = if (insn.type == Opcodes.F_APPEND) lastFrameSize else 0
                    lastFrameSize = frameSize

                    // localPos tracks the location in the frame node's locals list, which doesn't leave space for TOP entries
                    var localPos = 0
                    while (framePos < frame.size) {
                        // Get the local at the current position in the FrameNode's locals list
                        val localType = if ((localPos < insn.local.size)) insn.local[localPos] else null
                        if (localType is String) { // String refers to a reference type
                            frame[framePos] =
                                getLocalVariableAt(
                                    project,
                                    classNode,
                                    method,
                                    method.instructions.indexOf(insn),
                                    framePos
                                )
                        } else if (localType is Int) { // Integer refers to a primitive type or other marker
                            val isMarkerType = localType == Opcodes.UNINITIALIZED_THIS || localType == Opcodes.NULL
                            val is32bitValue = localType == Opcodes.INTEGER || localType == Opcodes.FLOAT
                            val is64bitValue = localType == Opcodes.DOUBLE || localType == Opcodes.LONG
                            if (localType == Opcodes.TOP) {
                                // Explicit TOP entries are pretty much always bogus, but depending on our resurrection
                                // strategy we may want to resurrect eligible zombies here. Real TOP entries are handled below
                                if (frame[framePos] is ZombieLocalVariable && settings.resurrectForBogusTop) {
                                    val zombie = frame[framePos] as ZombieLocalVariable
                                    if (zombie.type == ZombieLocalVariable.TRIM) {
                                        frame[framePos] = zombie.ancestor
                                    }
                                }
                            } else if (isMarkerType) {
                                frame[framePos] = null
                            } else if (is32bitValue || is64bitValue) {
                                frame[framePos] =
                                    getLocalVariableAt(
                                        project,
                                        classNode,
                                        method,
                                        method.instructions.indexOf(insn),
                                        framePos
                                    )
                                if (is64bitValue) {
                                    framePos++
                                    frame[framePos] = null // TOP
                                }
                            } else {
                                throw IllegalStateException(
                                    "Unrecognised locals opcode $localType in locals array at position" +
                                        " $localPos in ${classNode.name}.${method.name}${method.desc}"
                                )
                            }
                        } else if (localType == null) {
                            if ((framePos >= initialFrameSize) && (framePos >= frameSize) && (frameSize > 0)) {
                                if (framePos < knownFrameSize) {
                                    frame[framePos] = getLocalVariableAt(
                                        project,
                                        classNode,
                                        method,
                                        insn,
                                        framePos
                                    )
                                } else {
                                    frame[framePos] = ZombieLocalVariable.of(frame[framePos], ZombieLocalVariable.TRIM)
                                }
                            }
                        } else if (localType is LabelNode) {
                            // Uninitialised
                        } else {
                            throw IllegalStateException(
                                "Invalid value $localType in locals array at position" +
                                    " $localPos in ${classNode.name}.${method.name}${method.desc}"
                            )
                        }
                        framePos++
                        localPos++
                    }
                }

                handleFrame()
            } else if (insn is VarInsnNode) {
                val isLoad = insn.getOpcode() >= Opcodes.ILOAD && insn.getOpcode() <= Opcodes.SALOAD
                if (isLoad) {
                    val loadedVar = getLocalVariableAt(project, classNode, method, insn, insn.`var`)
                    frame[insn.`var`] = loadedVar
                    val varSize = loadedVar?.desc?.let { Type.getType(it).size } ?: 1
                    knownFrameSize = (insn.`var` + varSize).coerceAtLeast(knownFrameSize)
                    if (settings.resurrectExposedOnLoad) {
                        resurrect(frame, knownFrameSize, settings)
                    }
                } else {
                    // Update the LVT for the opcode AFTER this one, since we always want to know
                    // the frame state BEFORE the *current* instruction to match the contract of
                    // injection points
                    storeInsn = insn
                }
            }

            if (insn === node) {
                break
            }
        }

        // Null out any "unknown" or mixin-provided locals
        for (l in frame.indices) {
            val variable = frame[l]
            if (variable is ZombieLocalVariable) {
                // preserve zombies where the frame node which culled them was immediately prior to
                // the matched instruction, or *was itself* the matched instruction, the returned
                // frame will contain the original node (the zombie ancestor)
                frame[l] = if (variable.lifetime > 1) null else variable.ancestor
            }
            if (variable != null && variable.desc == null) {
                frame[l] = null
            }
        }

        return frame
    }

    private fun getAdjustedFrameSize(currentSize: Int, type: Int, size: Int, initialFrameSize: Int): Int {
        return when (type) {
            Opcodes.F_NEW, Opcodes.F_FULL -> size.coerceAtLeast(initialFrameSize)
            Opcodes.F_APPEND -> currentSize + size
            Opcodes.F_CHOP -> (size - currentSize).coerceAtLeast(initialFrameSize)
            Opcodes.F_SAME, Opcodes.F_SAME1 -> currentSize
            else -> currentSize
        }
    }

    private fun resurrect(frame: Array<LocalVariable?>, knownFrameSize: Int, settings: Settings) {
        for ((index, node) in frame.withIndex()) {
            if (index >= knownFrameSize) {
                break
            }
            if (node is ZombieLocalVariable && node.checkResurrect(settings)) {
                frame[index] = node.ancestor
            }
        }
    }

    private fun FrameNode.computeFrameSize(initialFrameSize: Int): Int {
        if (this.local == null) {
            return initialFrameSize
        }
        var size = 0
        for (local in this.local) {
            size += if (local == Opcodes.DOUBLE || local == Opcodes.LONG) 2 else 1
        }
        return size.coerceAtLeast(initialFrameSize)
    }

    private fun getLocalVariableAt(
        project: Project,
        classNode: ClassNode,
        method: MethodNode,
        pos: AbstractInsnNode,
        index: Int
    ): LocalVariable? {
        return getLocalVariableAt(project, classNode, method, method.instructions.indexOf(pos), index)
    }

    private fun getLocalVariableAt(
        project: Project,
        classNode: ClassNode,
        method: MethodNode,
        pos: Int,
        index: Int
    ): LocalVariable? {
        var localVariableNode: LocalVariable? = null
        var fallbackNode: LocalVariable? = null
        for (local in method.getLocalVariableTable(project, classNode)) {
            if (local.index != index) {
                continue
            }
            if (local.isInRange(pos)) {
                localVariableNode = local
            } else if (localVariableNode == null) {
                fallbackNode = local
            }
        }
        if (localVariableNode == null && method.localVariables.isNotEmpty()) {
            for (local in getGeneratedLocalVariableTable(project, classNode, method)) {
                if (local.index == index && local.isInRange(pos)) {
                    localVariableNode = local
                }
            }
        }
        return localVariableNode ?: fallbackNode
    }

    private fun InsnList.nextNode(insn: AbstractInsnNode): AbstractInsnNode {
        val index = indexOf(insn) + 1
        if (index > 0 && index < size()) {
            return get(index)
        }
        return insn
    }

    private fun MethodNode.getLocalVariableTable(project: Project, classNode: ClassNode): List<LocalVariable> {
        if (localVariables.isEmpty()) {
            return getGeneratedLocalVariableTable(project, classNode, this)
        }
        return localVariables.map {
            LocalVariable(
                it.name,
                it.desc,
                it.signature,
                instructions.indexOf(it.start),
                instructions.indexOf(it.end),
                it.index
            )
        }
    }

    private fun getGeneratedLocalVariableTable(
        project: Project,
        classNode: ClassNode,
        method: MethodNode
    ): List<LocalVariable> {
        val frames = AsmDfaUtil.analyzeMethod(project, classNode, method) ?: throw LocalAnalysisFailedException()

        // Record the original size of the method
        val methodSize = method.instructions.size()

        // List of LocalVariableNodes to return
        val localVariables = mutableListOf<LocalVariable>()

        // LocalVariableNodes for current frame
        val localVars = arrayOfNulls<LocalVariable>(method.maxLocals)

        // locals in previous frame, used to work out what changes between frames
        val locals = arrayOfNulls<BasicValue>(method.maxLocals)

        val lastKnownType = arrayOfNulls<String>(method.maxLocals)

        // Traverse the frames and work out when locals begin and end
        for (i in 0 until methodSize) {
            val f = frames[i] ?: continue
            for (j in 0 until f.locals) {
                val local = f.getLocal(j)
                if (local == null && locals[j] == null) {
                    continue
                }
                if (local != null && local == locals[j]) {
                    continue
                }
                if (local == null && locals[j] != null) {
                    val localVar = localVars[j]!!
                    localVariables.add(localVar)
                    localVar.end = i
                    localVars[j] = null
                } else if (local != null) {
                    if (locals[j] != null) {
                        val localVar = localVars[j]!!
                        localVariables.add(localVar)
                        localVar.end = i
                        localVars[j] = null
                    }
                    var desc = lastKnownType[j]
                    val localType = local.type
                    if (localType != null) {
                        desc = if (localType.sort >= Type.ARRAY && localType.internalName == "null") {
                            "Ljava/lang/Object;"
                        } else {
                            localType.descriptor
                        }
                    }
                    localVars[j] = LocalVariable("var$j", desc, null, i, null, j)
                    if (desc != null) {
                        lastKnownType[j] = desc
                    }
                }
                locals[j] = local
            }
        }

        // Reached the end of the method so flush all current locals and mark the end
        for (k in localVars.indices) {
            val localVar = localVars[k]
            if (localVar != null) {
                localVar.end = methodSize
                localVariables.add(localVar)
            }
        }

        return localVariables
    }

    data class Settings(
        val choppedInsnThreshold: Int,
        val trimmedInsnThreshold: Int,
        val choppedFrameThreshold: Int,
        val trimmedFrameThreshold: Int,
        val resurrectExposedOnLoad: Boolean,
        val resurrectExposedOnStore: Boolean,
        val resurrectForBogusTop: Boolean
    ) {
        companion object {
            val NO_RESURRECT = Settings(
                choppedInsnThreshold = 0,
                choppedFrameThreshold = 0,
                trimmedInsnThreshold = 0,
                trimmedFrameThreshold = 0,
                resurrectExposedOnLoad = false,
                resurrectExposedOnStore = false,
                resurrectForBogusTop = false
            )

            val DEFAULT = Settings(
                choppedInsnThreshold = -1,
                choppedFrameThreshold = 1,
                trimmedInsnThreshold = -1,
                trimmedFrameThreshold = -1,
                resurrectExposedOnLoad = true,
                resurrectExposedOnStore = true,
                resurrectForBogusTop = true
            )
        }
    }

    data class SourceLocalVariable(
        val name: String,
        val type: PsiType,
        val index: Int,
        val mixinName: String = name,
        val implicitLoadCountBefore: Int = 0,
        val implicitLoadCountAfter: Int = 0,
        val implicitStoreCountBefore: Int = 0,
        val implicitStoreCountAfter: Int = 0
    )

    open class LocalVariable(
        val name: String,
        val desc: String?,
        val signature: String?,
        val start: Int?,
        var end: Int?,
        val index: Int
    ) {
        fun isInRange(index: Int): Boolean {
            val end = this.end
            return (start == null || index >= start) && (end == null || index < end)
        }
    }

    private class LocalAnalysisFailedException : Exception() {
        override fun fillInStackTrace(): Throwable {
            return this
        }
    }

    private class ZombieLocalVariable private constructor(
        val ancestor: LocalVariable,
        val type: Char
    ) : LocalVariable(
        ancestor.name,
        ancestor.desc,
        ancestor.signature,
        ancestor.start,
        ancestor.end,
        ancestor.index
    ) {
        var lifetime = 0
        var frames = 0

        fun checkResurrect(settings: Settings): Boolean {
            val insnThreshold = if (type == CHOP) settings.choppedInsnThreshold else settings.trimmedInsnThreshold
            if (insnThreshold > -1 && lifetime > insnThreshold) {
                return false
            }
            val frameThreshold = if (type == CHOP) settings.choppedFrameThreshold else settings.trimmedFrameThreshold
            return frameThreshold == -1 || frames <= frameThreshold
        }

        override fun toString(): String {
            return String.format("Z(%s,%-2d)", type, lifetime)
        }

        companion object {
            const val CHOP = 'C'
            const val TRIM = 'X'

            fun of(ancestor: LocalVariable?, type: Char): ZombieLocalVariable? {
                return if (ancestor is ZombieLocalVariable) {
                    ancestor
                } else {
                    ancestor?.let { ZombieLocalVariable(it, type) }
                }
            }
        }
    }
}
