/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilder
import com.demonwav.mcdev.platform.mixin.inspection.addedMembers.AddedMembersNameFormatInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Added Members Name Format Inspection Tests")
class AddedMembersNameFormatInspectionTest : BaseMixinTest() {

    private fun doTest(
        @Language("JAVA") mixin: String,
        configureInspection: AddedMembersNameFormatInspection.() -> Unit = {},
        builder: ProjectBuilder.() -> Unit = {},
    ) {
        buildProject {
            dir("test") {
                java(
                    "ProjectType.java",
                    """
                    package test;

                    public class ProjectType {
                    }
                    """,
                    configure = false,
                )
                java(
                    "Duck.java",
                    """
                    package test;

                    public interface Duck {
                        void doSomething();
                        void doSomethingWithProjectType(ProjectType value);
                        ProjectType[] getProjectTypes();
                        java.util.List<ProjectType> getProjectTypeList();
                    }
                    """,
                    configure = false,
                )
                dir("api") {
                    java(
                        "ApiDuck.java",
                        """
                        package test.api;

                        public interface ApiDuck {
                            void doApiThing();
                        }
                        """,
                        configure = false,
                    )
                }
                java(
                    "InjectedDuck.java",
                    """
                    package test;

                    public interface InjectedDuck extends InjectedDuckSuper {
                        void doInjectedThing();

                        interface Inner {
                            void doInnerThing();
                        }
                    }
                    """,
                    configure = false,
                )
                java(
                    "InjectedDuckSuper.java",
                    """
                    package test;

                    public interface InjectedDuckSuper {
                        void doInjectedSuperThing();
                    }
                    """,
                    configure = false,
                )
                java(
                    "NonTransitiveDuck.java",
                    """
                    package test;

                    public interface NonTransitiveDuck {
                        void doNonTransitiveThing();
                    }
                    """,
                    configure = false,
                )
                builder()
                java("TestMixin.java", mixin)
            }
        }

        val inspection = AddedMembersNameFormatInspection()
        inspection.configureInspection()
        fixture.enableInspections(inspection)
        fixture.checkHighlighting(true, false, false)
    }

    private fun ProjectBuilder.classTweaker() {
        file(
            "test.classtweaker",
            """
            classTweaker v1 named
            transitive-inject-interface java/lang/String test/InjectedDuck
            transitive-inject-interface java/lang/String test/InjectedDuck${'$'}Inner
            inject-interface java/lang/String test/NonTransitiveDuck
            """,
            ".classtweaker",
            configure = false,
            allowAst = true,
        )
    }

    @Test
    @DisplayName("Unprefixed Duck Interface Method")
    fun unprefixedDuckInterfaceMethod() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(String.class)
            abstract class TestMixin implements Duck {
                @Override
                public void <warning descr="Name does not match the pattern for added mixin members: \".+[_$].+\"">doSomething</warning>() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("Project Type In Descriptor")
    fun projectTypeInDescriptor() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(String.class)
            abstract class TestMixin implements Duck {
                @Override
                public void doSomethingWithProjectType(ProjectType value) {
                }

                @Override
                public ProjectType[] getProjectTypes() {
                    return null;
                }

                @Override
                public java.util.List<ProjectType> <warning descr="Name does not match the pattern for added mixin members: \".+[_$].+\"">getProjectTypeList</warning>() {
                    return null;
                }
            }
            """
        )
    }

    @Test
    @DisplayName("Project Type In Descriptor Disabled")
    fun projectTypeInDescriptorDisabled() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(String.class)
            abstract class TestMixin implements Duck {
                @Override
                public void <warning descr="Name does not match the pattern for added mixin members: \".+[_$].+\"">doSomethingWithProjectType</warning>(ProjectType value) {
                }
            }
            """,
            configureInspection = { ignoreInheritedWithProjectTypeInDescriptor = false },
        )
    }

    @Test
    @DisplayName("API Interface")
    fun apiInterface() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import test.api.ApiDuck;

            @Mixin(String.class)
            abstract class TestMixin implements ApiDuck {
                @Override
                public void doApiThing() {
                }
            }
            """
        )
    }

    @Test
    @DisplayName("API Interface Disabled")
    fun apiInterfaceDisabled() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;
            import test.api.ApiDuck;

            @Mixin(String.class)
            abstract class TestMixin implements ApiDuck {
                @Override
                public void <warning descr="Name does not match the pattern for added mixin members: \".+[_$].+\"">doApiThing</warning>() {
                }
            }
            """,
            configureInspection = { ignoreInheritedFromApiInterfaces = false },
        )
    }

    @Test
    @DisplayName("Transitively Injected Interface")
    fun transitivelyInjectedInterface() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(String.class)
            abstract class TestMixin implements InjectedDuck, InjectedDuck.Inner, NonTransitiveDuck {
                @Override
                public void doInjectedThing() {
                }

                @Override
                public void doInjectedSuperThing() {
                }

                @Override
                public void doInnerThing() {
                }

                @Override
                public void <warning descr="Name does not match the pattern for added mixin members: \".+[_$].+\"">doNonTransitiveThing</warning>() {
                }
            }
            """,
            builder = { classTweaker() },
        )
    }

    @Test
    @DisplayName("Transitively Injected Interface Disabled")
    fun transitivelyInjectedInterfaceDisabled() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(String.class)
            abstract class TestMixin implements InjectedDuck {
                @Override
                public void <warning descr="Name does not match the pattern for added mixin members: \".+[_$].+\"">doInjectedThing</warning>() {
                }

                @Override
                public void <warning descr="Name does not match the pattern for added mixin members: \".+[_$].+\"">doInjectedSuperThing</warning>() {
                }
            }
            """,
            configureInspection = { ignoreInheritedFromTransitivelyInjectedInterfaces = false },
            builder = { classTweaker() },
        )
    }
}
