package com.mumfrey.ipviewer.lib.mock;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.refmap.IMixinContext;

/**
 * TODO Description for Method
 */
public class Method {
    
    private final IMixinContext owner;
    private final String name;
    private final String desc;
    private final boolean isStatic;

    public Method(IMixinContext owner, String name, String desc, boolean isStatic) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.isStatic = isStatic;
    }
    
    public IMixinContext getOwner() {
        return this.owner;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getDesc() {
        return this.desc;
    }
    
    public boolean isStatic() {
        return this.isStatic;
    }

    public MethodNode asMethodNode() {
        return new MethodNode(Opcodes.ACC_PUBLIC | (this.isStatic ? Opcodes.ACC_STATIC : 0), this.name, this.desc, null, null);
    }
    
}
