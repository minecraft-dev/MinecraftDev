/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.debug

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.jdi.VirtualMachineProxyImpl
import com.intellij.openapi.module.ModulePointer
import com.intellij.xdebugger.XDebugSessionListener
import com.sun.jdi.BooleanValue
import com.sun.jdi.ClassType
import com.sun.jdi.ObjectReference

class UngrabMouseDebugSessionListener(private val process: DebugProcessImpl, private val modulePointer: ModulePointer) :
    XDebugSessionListener {

    private fun ungrabMouse() {
        val suspendContextImpl = process.debuggerContext.suspendContext ?: return
        if (suspendContextImpl.thread?.isAtBreakpoint != true) {
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
        val srgMap = modulePointer.module?.let { module ->
            MinecraftFacet.getInstance(module, McpModuleType)?.srgManager?.srgMapNow
        }

        fun mcp(srg: String, mcp: String): String {
            return srgMap?.getMcpClass(srg) ?: mcp
        }

        val minecraftClass = virtualMachine.classesByName("net.minecraft.client.Minecraft")
            ?.singleOrNull() as? ClassType ?: return
        val minecraftGetter =
            minecraftClass.methodsByName(mcp("func_71410_x", "getInstance"), "()Lnet/minecraft/client/Minecraft;")
                ?.singleOrNull() ?: return
        val minecraft = debugProcess.invokeMethod(
            evaluationContext,
            minecraftClass,
            minecraftGetter,
            emptyList()
        ) as? ObjectReference ?: return

        val mouseHelperField = minecraftClass.fieldByName(mcp("field_71417_B", "mouseHelper")) ?: return
        val mouseHelper = minecraft.getValue(mouseHelperField) as? ObjectReference ?: return

        val mouseHelperClass =
            virtualMachine.classesByName("net.minecraft.client.MouseHelper")?.singleOrNull() as? ClassType ?: return
        val grabMouse =
            mouseHelperClass.methodsByName(mcp("func_198032_j", "ungrabMouse"), "()V")?.singleOrNull() ?: return

        debugProcess.invokeMethod(evaluationContext, mouseHelper, grabMouse, emptyList())
    }

    override fun sessionPaused() {
        ungrabMouse()
    }
}
