/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.mumfrey.ipviewer.lib;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.LineNumberNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.InjectorTarget;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.ASMHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * TODO Description for Injection
 */
public class Injection {
    
    private final TargetClass targetClass;
    private final Injector injector;
    private final IMixinContext mixin;
    private final Deque<MethodNode> targets = new ArrayDeque<MethodNode>();
    protected final Map<Target, List<InjectionNode>> targetNodes = new LinkedHashMap<Target, List<InjectionNode>>();

    public Injection(TargetClass viewer, Injector injector) {
        this.targetClass = viewer;
        this.injector = injector;
        this.mixin = injector.getContext();
        this.findMethods();
    }
    
    private void findMethods() {
        MemberInfo searchFor = this.injector.target();
        
        this.targets.clear();
        int ordinal = 0;
        
        for (MethodNode target : this.targetClass.getMethods()) {
            if (searchFor.matches(target.name, target.desc, ordinal)) {
                boolean isMixinMethod = ASMHelper.getVisibleAnnotation(target, MixinMerged.class) != null;
                if (searchFor.matchAll && (ASMHelper.methodIsStatic(target) != this.injector.method().isStatic() || isMixinMethod)) {
                    continue;
                }
                
                this.targets.add(target);
                ordinal++;
            }
        }
    }

    public void run() {
        this.targetNodes.clear();
        for (MethodNode targetMethod : this.targets) {
            Target target = this.mixin.getTargetMethod(targetMethod);
            InjectorTarget injectorTarget = new InjectorTarget(this.injector, target);
            this.targetNodes.put(target, this.find(injectorTarget, this.injector.injectionPoints()));
            injectorTarget.dispose();
        }
    }
    
    private final List<InjectionNode> find(InjectorTarget injectorTarget, List<InjectionPoint> injectionPoints) {
        List<InjectionNode> myNodes = new ArrayList<InjectionNode>();
        for (AbstractInsnNode node : this.findTargetNodes(injectorTarget, injectionPoints)) {
            myNodes.add(injectorTarget.getTarget().injectionNodes.add(node));
        }
        return myNodes;
    }

    private Collection<AbstractInsnNode> findTargetNodes(InjectorTarget injectorTarget, List<InjectionPoint> injectionPoints) {
        List<AbstractInsnNode> targetNodes = new ArrayList<AbstractInsnNode>();
        Collection<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>(32);

        for (InjectionPoint injectionPoint : injectionPoints) {
            nodes.clear();

            if (!injectionPoint.find(injectorTarget.getMethod().desc, injectorTarget.getSlice(injectionPoint), nodes)) {
                continue;
            }
            
            for (AbstractInsnNode node : nodes) {
                if (!targetNodes.contains(node)) {
                    targetNodes.add(node);
                }
            }
        }
        
        return targetNodes;
    }

    public void print() {
        if (this.targetNodes.size() > 0) {
            System.err.printf("Injector: %s\n", this.injector);
            for (Entry<Target, List<InjectionNode>> entry : this.targetNodes.entrySet()) {
                Target target = entry.getKey();
                System.err.printf("Target: %s\n", target);
                List<InjectionNode> nodes = entry.getValue();
                for (InjectionNode node : nodes) {
                    System.err.printf("Injection: Opcode=%s Line=%d\n", ASMHelper.getOpcodeName(node.getCurrentTarget()),
                            this.getLineNumber(target, node));
                }
            }
        }
    }

    private Object getLineNumber(Target target, InjectionNode node) {
        LabelNode last = null;
        
        for (AbstractInsnNode insn : target) {
            if (insn instanceof LabelNode) {
                last = (LabelNode)insn;
            } else if (insn == node.getCurrentTarget()) {
                break;
            }
        }
        
        if (last != null) {
            for (AbstractInsnNode insn : target) {
                if (insn instanceof LineNumberNode) {
                    LineNumberNode line = (LineNumberNode)insn;
                    if (line.start == last) {
                        return line.line;
                    }
                }
            }
        }

        return 0;
    }
}
