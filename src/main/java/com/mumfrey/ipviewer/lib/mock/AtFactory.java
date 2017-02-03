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
import org.spongepowered.asm.mixin.injection.At.Shift;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;

/**
 * Factory for creating {@link At} instances at runtime
 */
public class AtFactory {
    
    /**
     * Implementation of {@link At}
     */
    static class AtImpl implements At {

        private final String value;
        private final String slice;
        private final Shift shift;
        private final int by;
        private final String[] args;
        private final String target;
        private final int ordinal;
        private final int opcode;
        private final boolean remap;
        
        public AtImpl(String value, String slice, Shift shift, int by, String[] args, String target, int ordinal, int opcode, boolean remap) {
            this.value = value;
            this.slice = slice;
            this.shift = shift;
            this.by = by;
            this.args = args != null ? args : new String[0];
            this.target = target;
            this.ordinal = ordinal;
            this.opcode = opcode;
            this.remap = remap;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return At.class;
        }

        @Override
        public String value() {
            return this.value;
        }

        @Override
        public String slice() {
            return this.slice;
        }

        @Override
        public Shift shift() {
            return this.shift;
        }

        @Override
        public int by() {
            return this.by;
        }

        @Override
        public String[] args() {
            return this.args;
        }

        @Override
        public String target() {
            return this.target;
        }

        @Override
        public int ordinal() {
            return this.ordinal;
        }

        @Override
        public int opcode() {
            return this.opcode;
        }

        @Override
        public boolean remap() {
            return this.remap;
        }
        
    }
    
    /**
     * Builder for {@link At} instances
     */
    public static class Builder {
        
        private String value;
        private String slice;
        private Shift shift;
        private int by;
        private String[] args;
        private String target;
        private int ordinal = -1;
        private int opcode;
        private boolean remap;
        
        private Consumer<At> consumer;
        
        Builder() {
        }
        
        Builder(Consumer<At> consumer) {
            this.consumer = consumer;
        }
        
        public Builder value(String value) {
            this.value = value;
            return this;
        }
        
        public Builder slice(String slice) {
            this.slice = slice;
            return this;
        }
        
        public Builder shift(Shift shift) {
            this.shift = shift;
            return this;
        }
        
        public Builder by(int by) {
            this.by = by;
            return this;
        }
        
        public Builder args(String... args) {
            this.args = args;
            return this;
        }
        
        public Builder target(String target) {
            this.target = target;
            return this;
        }
        
        public Builder ordinal(int ordinal) {
            this.ordinal = ordinal;
            return this;
        }
        
        public Builder opcode(int opcode) {
            this.opcode = opcode;
            return this;
        }
        
        public Builder remap(boolean remap) {
            this.remap = remap;
            return this;
        }
        
        public At build() {
            AtFactory.AtImpl at = new AtFactory.AtImpl(this.value, this.slice, this.shift, this.by, this.args, this.target, this.ordinal, this.opcode,
                                                       this.remap);
            if (this.consumer != null) {
                this.consumer.accept(at);
            }
            return at;
        }

    }

    private AtFactory() {

    }

    public static AtFactory.Builder builder() {
        return new AtFactory.Builder();
    }

}
