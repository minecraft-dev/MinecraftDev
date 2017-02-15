/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.example.test;

class OuterClass {

    private static final Object ANONYMOUS_CLASS = new Object() {

    };

    class InnerClass {

        public void test() {
            new Object() {

                class AnonymousInnerClass {

                }

            };
        }

    }

}
