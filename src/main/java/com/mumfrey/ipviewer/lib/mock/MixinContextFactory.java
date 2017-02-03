/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.mumfrey.ipviewer.lib.mock;

import com.mumfrey.ipviewer.lib.TargetClass;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;

import java.util.Collections;
import java.util.List;

/**
 * TODO Description for MixinContextFactory
 */
public class MixinContextFactory {
    
    /**
     * TODO Description for Info
     */
    static class Info implements IMixinInfo {
        
        private final String name, ref;
        
        public Info(String name) {
            this.name = name;
            this.ref = name.replace('.', '/');
        }

        @Override
        public IMixinConfig getConfig() {
            return null;
        }

        @Override
        public String getName() {
            return this.name.substring(this.name.lastIndexOf('.') + 1);
        }

        @Override
        public String getClassName() {
            return this.name;
        }

        @Override
        public String getClassRef() {
            return this.ref;
        }

        @Override
        public byte[] getClassBytes() {
            throw new UnsupportedOperationException("MixinInfo::getClassBytes is not supported");
        }

        @Override
        public boolean isDetachedSuper() {
            return false;
        }

        @Override
        public ClassNode getClassNode(int flags) {
            throw new UnsupportedOperationException("MixinInfo::getClassNode is not supported");
        }

        @Override
        public List<String> getTargetClasses() {
            return Collections.<String>emptyList();
        }

        @Override
        public int getPriority() {
            return IMixinConfig.DEFAULT_PRIORITY;
        }

        @Override
        public Phase getPhase() {
            return Phase.DEFAULT;
        }
        
    }
    
    /**
     * TODO Description for MixinContext
     */
    static class MixinContext implements IMixinContext {
        
        private final IMixinInfo info;
        
        private final TargetClass target;

        public MixinContext(String name, TargetClass target) {
            this.info = new Info(name);
            this.target = target;
        }
        
        @Override
        public String toString() {
            return this.info.getClassName();
        }

        @Override
        public IMixinInfo getMixin() {
            return this.info;
        }

        @Override
        public String getClassRef() {
            return this.info.getClassRef();
        }

        @Override
        public ReferenceMapper getReferenceMapper() {
            return null;
        }

        @Override
        public boolean getOption(Option option) {
            return false;
        }

        @Override
        public int getPriority() {
            return this.info.getPriority();
        }

        @Override
        public Target getTargetMethod(MethodNode method) {
            return this.target.getTargetMethod(method);
        }
        
    }
    
    /**
     * TODO Description for Builder
     */
    public static class Builder {
        
        private String name;
        private TargetClass target;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder target(TargetClass target) {
            this.target = target;
            return this;
        }
        
        public IMixinContext build() {
            return new MixinContext(this.name, this.target);
        }

    }
    
    public static Builder builder() {
        return new MixinContextFactory.Builder();
    }

}
