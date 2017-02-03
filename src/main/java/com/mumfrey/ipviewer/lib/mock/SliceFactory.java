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

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.Annotation;

/**
 * Factory for creating {@link Slice} instances at runtime
 */
public class SliceFactory {
    
    public static At DEFAULT_SLICE = AtFactory.builder().value("HEAD").build();
    
    /**
     * Implementation of {@link Slice}
     */
    static class SliceImpl implements Slice {

        private final String id;
        private final At from;
        private final At to;
        
        SliceImpl(String id, At from, At to) {
            this.id = id != null ? id : "";
            this.from = from != null ? from : SliceFactory.DEFAULT_SLICE;
            this.to = to != null ? to : SliceFactory.DEFAULT_SLICE;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Slice.class;
        }

        @Override
        public String id() {
            return this.id;
        }

        @Override
        public At from() {
            return this.from;
        }

        @Override
        public At to() {
            return this.to;
        }
        
    }
    
    /**
     * Builder for {@link Slice} instances
     */
    public static class Builder {
        
        private String id = "";
        private At from = SliceFactory.DEFAULT_SLICE;
        private At to = SliceFactory.DEFAULT_SLICE;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder from(At from) {
            this.from = from;
            return this;
        }
        
        public AtFactory.Builder from() {
            return new AtFactory.Builder((at) -> this.from(at));
        }
        
        public Builder to(At to) {
            this.to = to;
            return this;
        }
        
        public AtFactory.Builder to() {
            return new AtFactory.Builder((at) -> this.to(at));
        }
        
        public Slice build() {
            return new SliceFactory.SliceImpl(this.id, this.from, this.to);
        }

    }

    private SliceFactory() {

    }

    public static SliceFactory.Builder builder() {
        return new SliceFactory.Builder();
    }
}
