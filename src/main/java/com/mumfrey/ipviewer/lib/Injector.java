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

import com.mumfrey.ipviewer.lib.mock.Method;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.code.ISliceContext;
import org.spongepowered.asm.mixin.injection.code.MethodSlice;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.injection.throwables.InvalidSliceException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.ASMHelper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO Description for Injector
 */
public class Injector implements ISliceContext {
    
    private final IMixinContext mixin;
    private final Method method;
    private final AnnotationNode annotation;
    private final Map<String, MethodSlice> slices = new HashMap<String, MethodSlice>(4);
    private final List<InjectionPoint> injectionPoints = new ArrayList<InjectionPoint>();

    private String target = "";
    private boolean multiSlice = false;

    public Injector(IMixinContext mixin, Method method, Class<? extends Annotation> annotation) {
        this.mixin = mixin;
        this.method = method;
        this.annotation = new AnnotationNode("L" + annotation.getName().replace('.', '/') + ";");
    }
    
    public Injector setTarget(String target) {
        this.target = target;
        return this;
    }
    
    public Injector setMultiSlice(boolean multiSlice) {
        this.multiSlice = multiSlice;
        return this;
    }
    
    public Injector addInjectionPoint(At at) {
        InjectionPoint injectionPoint = InjectionPoint.parse(this, at);
        this.injectionPoints.add(injectionPoint);
        return this;
    }
    
    public Injector addSlice(Slice slice) {
        this.addSlice(MethodSlice.parse(this, slice));
        return this;
    }
    
    @Override
    public String toString() {
        MethodNode md = this.method.asMethodNode();
        return String.format("%s->@%s::%s%s", this.mixin.toString(), ASMHelper.getSimpleName(this.annotation), md.name, md.desc);
    }

    private void addSlice(MethodSlice slice) {
        String id = this.getSliceId(slice.getId());
        if (this.slices.containsKey(id)) {
            throw new InvalidSliceException(this, slice + " has a duplicate id, '" + id + "' was already defined");
        }
        this.slices.put(id, slice);
    }
    
    private String getSliceId(String id) {
        return this.multiSlice ? id : ""; 
    }
    
    public MemberInfo target() {
        return MemberInfo.parse(this.target, this.mixin);
    }
    
    public Method method() {
        return this.method;
    }
    
    public List<InjectionPoint> injectionPoints() {
        return this.injectionPoints;
    }

    @Override
    public MethodSlice getSlice(String id) {
        return this.slices.get(this.getSliceId(id));
    }

    @Override
    public IMixinContext getContext() {
        return this.mixin;
    }

    @Override
    public MethodNode getMethod() {
        return this.method.asMethodNode();
    }

    @Override
    public AnnotationNode getAnnotation() {
        return this.annotation;
    }

}
