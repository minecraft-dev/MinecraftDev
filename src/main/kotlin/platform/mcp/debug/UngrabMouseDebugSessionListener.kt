/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.debug

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.VirtualMachineProxyImpl
import com.intellij.xdebugger.XDebugSessionListener
import com.sun.jdi.BooleanValue
import com.sun.jdi.ClassType
import com.sun.jdi.Field
import com.sun.jdi.Method
import com.sun.jdi.ObjectReference
import com.sun.jdi.ReferenceType

class UngrabMouseDebugSessionListener(private val process: DebugProcessImpl) : XDebugSessionListener {

    private fun ungrabMouse() {
        val suspendContextImpl = process.debuggerContext.suspendContext ?: return
        if (suspendContextImpl.thread?.isSuspended != true) {
            return
        }

        val frameProxy = suspendContextImpl.frameProxy ?: return
        val debugProcess = suspendContextImpl.debugProcess
        val virtualMachine = debugProcess.virtualMachineProxy as? VirtualMachineProxyImpl ?: return
        val evaluationContext = EvaluationContextImpl(suspendContextImpl, frameProxy)

        val mouseClass = virtualMachine.classesByName("org.lwjgl.input.Mouse")?.singleOrNull() as? ClassType
        // LWJGL 3 does not have the Mouse class, Minecraft uses its own MouseHelper instead
        if (mouseClass != null) {
            ungrab2(mouseClass, virtualMachine, debugProcess, evaluationContext)
        } else {
            ungrab3(virtualMachine, debugProcess, evaluationContext)
        }
    }

    private fun ungrab2(
        mouseClass: ClassType,
        virtualMachine: VirtualMachineProxyImpl,
        debugProcess: DebugProcessImpl,
        evaluationContext: EvaluationContextImpl
    ) {
        val isGrabbed = mouseClass.methodsByName("isGrabbed", "()Z")?.singleOrNull() ?: return
        val setGrabbed = mouseClass.methodsByName("setGrabbed", "(Z)V")?.singleOrNull() ?: return
        val grabValue = virtualMachine.mirrorOf(false)

        val currentState = debugProcess.invokeMethod(evaluationContext, mouseClass, isGrabbed, emptyList())
        if ((currentState as? BooleanValue)?.value() == true) {
            debugProcess.invokeMethod(evaluationContext, mouseClass, setGrabbed, listOf(grabValue))
        }
    }

    private fun ungrab3(
        virtualMachine: VirtualMachineProxyImpl,
        debugProcess: DebugProcessImpl,
        evaluationContext: EvaluationContextImpl
    ) {
        fun findClass(vararg names: String): ClassType? {
            for (name in names) {
                (virtualMachine.classesByName(name)?.singleOrNull() as? ClassType)?.let { return it }
            }
            return null
        }

        fun ClassType.fieldByName(vararg names: String): Field? {
            for (name in names) {
                this.fieldByName(name)?.let { return it }
            }
            return null
        }

        fun ReferenceType.methodByName(vararg names: Pair<String, String>): Method? {
            for ((name, signature) in names) {
                this.methodsByName(name, signature)?.singleOrNull()?.let { return it }
            }
            return null
        }

        val minecraftClass = findClass(
            "net.minecraft.client.Minecraft",
            "net.minecraft.client.MinecraftClient"
        ) ?: return
        val minecraftGetter = minecraftClass.methodByName(
            "getInstance" to "()Lnet/minecraft/client/Minecraft;",
            "getInstance" to "()Lnet/minecraft/client/MinecraftClient;"
        ) ?: return
        val minecraft = debugProcess.invokeMethod(
            evaluationContext,
            minecraftClass,
            minecraftGetter,
            emptyList()
        ) as? ObjectReference ?: return

        val mouseHelperField = minecraftClass.fieldByName("mouseHandler", "mouse", "mouseHelper") ?: return
        val mouseHelper = minecraft.getValue(mouseHelperField) as? ObjectReference ?: return

        val ungrabMouse = mouseHelper.referenceType().methodByName(
            "releaseMouse" to "()V",
            "unlockCursor" to "()V",
            "ungrabMouse" to "()V"
        ) ?: return

        debugProcess.invokeMethod(evaluationContext, mouseHelper, ungrabMouse, emptyList())
    }

    override fun sessionPaused() {
        ungrabMouse()
    }
}
