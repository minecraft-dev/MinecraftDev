package com.mumfrey.ipviewer.lib;

import com.mumfrey.ipviewer.lib.mock.MixinContextFactory;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO Description for TargetClass
 */
public class TargetClass {
    
    private final ClassNode classNode = new ClassNode();
    private final Map<String, Target> targetMethods = new HashMap<String, Target>();
    private final List<Injector> injectors = new ArrayList<Injector>();
    private final List<Injection> injections = new ArrayList<Injection>();
    private List<IMixinContext> mixins = new ArrayList<IMixinContext>();

    public TargetClass(byte[] bytes) {
        new ClassReader(bytes).accept(this.classNode, 0);
    }

    public void addInjector(Injector injector) {
        this.injectors.add(injector);
    }

    public void runInjectors() {
        for (Injector injector : this.injectors) {
            Injection injection = new Injection(this, injector);
            this.injections.add(injection);
            injection.run();
        }
        
        for (Injection injection : this.injections) {
            injection.print();
        }
    }
    
    List<MethodNode> getMethods() {
        return this.classNode.methods;
    }

    public IMixinContext addMixin(String owner) {
        IMixinContext mixin = MixinContextFactory.builder().name(owner).target(this).build();
        this.mixins.add(mixin);
        return mixin;
    }

    public Target getTargetMethod(MethodNode method) {
        if (!this.classNode.methods.contains(method)) {
            throw new IllegalArgumentException("Invalid target method supplied to getTargetMethod()");
        }
        
        String targetName = method.name + method.desc;
        Target target = this.targetMethods.get(targetName);
        if (target == null) {
            target = new Target(this.classNode, method);
            this.targetMethods.put(targetName, target);
        }

        return target;
    }

}
