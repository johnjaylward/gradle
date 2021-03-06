/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.managed

import org.gradle.api.artifacts.Configuration
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import spock.lang.Unroll

class ScalarTypesInManagedModelIntegrationTest extends AbstractIntegrationSpec {

    def "values of primitive types and boxed primitive types are widened as usual when using groovy"() {
        when:
        buildScript '''
            @Managed
            interface PrimitiveTypes {
                Long getLongPropertyFromInt()
                void setLongPropertyFromInt(Long value)

                Long getLongPropertyFromInteger()
                void setLongPropertyFromInteger(Long value)
            }

            class RulePlugin extends RuleSource {
                @Model
                void createPrimitiveTypes(PrimitiveTypes primitiveTypes) {
                    primitiveTypes.longPropertyFromInt = 123
                    primitiveTypes.longPropertyFromInteger = new Integer(321)
                }

                @Mutate
                void addEchoTask(ModelMap<Task> tasks, final PrimitiveTypes primitiveTypes) {
                    tasks.create("echo") {
                        it.doLast {
                            println "from int: $primitiveTypes.longPropertyFromInt"
                            println "from Integer: $primitiveTypes.longPropertyFromInteger"
                        }
                    }
                }
            }

            apply type: RulePlugin
        '''

        then:
        succeeds "echo"

        and:
        output.contains "from int: 123"
        output.contains "from Integer: 321"
    }

    def "values of primitive types are boxed as usual when using java"() {
        when:
        file('buildSrc/src/main/java/Rules.java') << '''
            import org.gradle.api.*;
            import org.gradle.model.*;

            @Managed
            interface PrimitiveProperty {
                Long getLongProperty();
                void setLongProperty(Long value);
                Integer getIntegerProperty();
                void setIntegerProperty(Integer value);
                Boolean getBooleanProperty();
                void setBooleanProperty(Boolean value);
                Character getCharacterProperty();
                void setCharacterProperty(Character value);
            }

            class RulePlugin extends RuleSource {
                @Model
                void createPrimitiveProperty(PrimitiveProperty primitiveProperty) {
                    primitiveProperty.setLongProperty(123l);
                    primitiveProperty.setIntegerProperty(456);
                    primitiveProperty.setBooleanProperty(true);
                    primitiveProperty.setCharacterProperty('a');
                }

                @Mutate
                void addEchoTask(ModelMap<Task> tasks, final PrimitiveProperty primitiveProperty) {
                    tasks.create("echo", new Action<Task>() {
                        public void execute(Task task) {
                            task.doLast(new Action<Task>() {
                                public void execute(Task unused) {
                                    System.out.println(String.format("long: %d", primitiveProperty.getLongProperty()));
                                    System.out.println(String.format("integer: %d", primitiveProperty.getIntegerProperty()));
                                    System.out.println(String.format("boolean: %s", primitiveProperty.getBooleanProperty()));
                                    System.out.println(String.format("character: %s", primitiveProperty.getCharacterProperty()));
                                }
                            });
                        }
                    });
                }
            }
        '''

        buildScript '''
            apply type: RulePlugin
        '''

        then:
        succeeds "echo"

        and:
        output.contains "long: 123"
        output.contains "integer: 456"
        output.contains "boolean: true"
        output.contains "character: a"
    }

    def "can set/get properties of all supported unmanaged types"() {
        when:
        buildScript '''
            @Managed
            interface AllSupportedUnmanagedTypes {
                Boolean getBooleanProperty()

                void setBooleanProperty(Boolean value)

                Integer getIntegerProperty()

                void setIntegerProperty(Integer value)

                Long getLongProperty()

                void setLongProperty(Long value)

                Double getDoubleProperty()

                void setDoubleProperty(Double value)

                BigInteger getBigIntegerProperty()

                void setBigIntegerProperty(BigInteger value)

                BigDecimal getBigDecimalProperty()

                void setBigDecimalProperty(BigDecimal value)

                String getStringProperty()

                void setStringProperty(String value)

                File getFile()

                void setFile(File file)
            }

            class RulePlugin extends RuleSource {
                @Model
                void supportedUnmanagedTypes(AllSupportedUnmanagedTypes element) {
                    element.booleanProperty = Boolean.TRUE
                    element.integerProperty = Integer.valueOf(1)
                    element.longProperty = Long.valueOf(2L)
                    element.doubleProperty = Double.valueOf(3.3)
                    element.bigIntegerProperty = new BigInteger("4")
                    element.bigDecimalProperty = new BigDecimal("5.5")
                    element.stringProperty = "test"
                    element.file = new File('sample.txt')
                }

                @Mutate
                void addEchoTask(ModelMap<Task> tasks, AllSupportedUnmanagedTypes element) {
                    tasks.create("echo") {
                        it.doLast {
                            println "boolean: ${element.booleanProperty}"
                            println "integer: ${element.integerProperty}"
                            println "long: ${element.longProperty}"
                            println "double: ${element.doubleProperty}"
                            println "big integer: ${element.bigIntegerProperty}"
                            println "big decimal: ${element.bigDecimalProperty}"
                            println "string: ${element.stringProperty}"
                            println "file: ${element.file}"
                        }
                    }
                }
            }

            apply type: RulePlugin
        '''

        then:
        succeeds "echo"

        and:
        output.contains "boolean: true"
        output.contains "integer: 1"
        output.contains "long: 2"
        output.contains "double: 3.3"
        output.contains "big integer: 4"
        output.contains "big decimal: 5.5"
        output.contains "string: test"
        output.contains "file: sample.txt"
    }

    def "can specify managed models with file types"() {
        when:
        buildScript '''
            @Managed
            interface FileContainer {
                File getFile()
                void setFile(File file)
            }

            model {
                gradleFileContainer(FileContainer) {
                    file = file('sample.txt')
                }

                jdkFileContainer(FileContainer) {
                    file = new File('sample.txt')
                }
            }
        '''

        then:
        succeeds "model"
    }

    @Unroll
    def "can read and write #value to managed property of scalar type #someType.simpleName when using Groovy"() {
        given:
        buildScript """
            import org.gradle.api.artifacts.Configuration.State

            @Managed
            interface ManagedType {
                $someType.canonicalName getManagedProperty()

                void setManagedProperty($someType.canonicalName value)
            }

            class PluginRules extends RuleSource {
                @Model
                void createModel(ManagedType p) {
                    p.managedProperty = $value
                }

                @Mutate
                void addEchoTask(ModelMap<Task> tasks, ManagedType element) {
                    tasks.create("echo") {
                        it.doLast {
                            println "${someType.name}: \${element.managedProperty}"
                        }
                    }
                }
            }

            apply plugin: PluginRules

        """

        when:
        succeeds 'echo'

        then:
        output.contains "${someType.name}: ${Eval.me(value)}"

        where:
        someType            | value
        byte                | "123"
        Byte                | "123"
        boolean             | "false"
        Boolean             | "false"
        boolean             | "true"
        Boolean             | "true"
        char                | "'c'"
        Character           | "'c'"
        float               | "123.45f"
        Float               | "123.45f"
        long                | "123L"
        Long                | "123L"
        short               | "123"
        Short               | "123"
        int                 | "123"
        Integer             | "123"
        double              | "123.456d"
        Double              | "123.456d"
        String              | '"Mogette"'
        BigDecimal          | '999G'
        BigInteger          | '777G'
        Configuration.State | 'org.gradle.api.artifacts.Configuration.State.UNRESOLVED'
        File                | 'new File("foo")'
    }

    @Unroll
    def "can read and write #value to managed property of scalar type #someType,simpleName when using Java"() {
        given:
        file('buildSrc/src/main/java/Rules.java') << """
            import org.gradle.api.*;
            import org.gradle.model.*;
            import java.math.BigDecimal;
            import java.math.BigInteger;
            import java.io.File;
            import org.gradle.api.artifacts.Configuration.State;

            @Managed
            interface ManagedType {
                $someType.canonicalName getManagedProperty();

                void setManagedProperty($someType.canonicalName value);
            }

            class RulePlugin extends RuleSource {
                @Model
                void createModel(ManagedType p) {
                    p.setManagedProperty($value);
                }

                @Mutate
                void addEchoTask(ModelMap<Task> tasks, final ManagedType element) {
                    tasks.create("echo", new Action<Task>() {
                        public void execute(Task task) {
                            task.doLast(new Action<Task>() {
                                public void execute(Task unused) {
                                    System.out.println(String.format("%s: %s", "${someType.name}", element.getManagedProperty()));
                                }
                            });
                        }
                    });
                }
            }
        """

        when:
        buildScript '''
            apply type: RulePlugin
        '''

        then:
        succeeds 'echo'

        and:
        output.contains "${someType.name}: ${Eval.me(value)}"


        where:
        someType            | value
        byte                | "(byte) 123"
        Byte                | "(byte) 123"
        boolean             | "false"
        Boolean             | "false"
        boolean             | "true"
        Boolean             | "true"
        char                | "'c'"
        Character           | "'c'"
        float               | "123.45f"
        Float               | "123.45f"
        long                | "123L"
        Long                | "123L"
        short               | "(short) 123"
        Short               | "(short) 123"
        int                 | "123"
        Integer             | "123"
        double              | "123.456d"
        Double              | "123.456d"
        String              | '"Mogette"'
        BigDecimal          | 'new BigDecimal(999)'
        BigInteger          | 'new BigInteger("777")'
        Configuration.State | 'org.gradle.api.artifacts.Configuration.State.UNRESOLVED'
        File                | 'new File("foo")'
    }

    @Unroll
    def "cannot mutate managed property of scalar type #someType.simpleName when view is immutable"() {
        given:
        buildScript """
            import org.gradle.api.artifacts.Configuration.State

            @Managed
            interface ManagedType {
                $someType.canonicalName getManagedProperty()

                void setManagedProperty($someType.canonicalName value)
            }

            class PluginRules extends RuleSource {
                @Model
                void createModel(ManagedType p) {
                }

                @Mutate
                void addEchoTask(ModelMap<Task> tasks, ManagedType element) {
                    element.managedProperty = $value
                }
            }

            apply plugin: PluginRules

        """

        when:
        fails 'echo'

        then:
        failure.assertHasCause(/Attempt to mutate closed view of model of type 'ManagedType' given to rule 'PluginRules#addEchoTask'/)

        where:
        someType            | value
        byte                | "123"
        Byte                | "123"
        boolean             | "false"
        Boolean             | "false"
        boolean             | "true"
        Boolean             | "true"
        char                | "'c'"
        Character           | "'c'"
        float               | "123.45f"
        Float               | "123.45f"
        long                | "123L"
        Long                | "123L"
        short               | "123"
        Short               | "123"
        int                 | "123"
        Integer             | "123"
        double              | "123.456d"
        Double              | "123.456d"
        String              | '"Mogette"'
        BigDecimal          | '999G'
        BigInteger          | '777G'
        Configuration.State | 'org.gradle.api.artifacts.Configuration.State.UNRESOLVED'
        File                | 'new File("foo")'
    }

    @Unroll
    def "cannot have read only property of scalar type #someType.simpleName"() {
        given:
        buildScript """
            import org.gradle.api.artifacts.Configuration.State

            @Managed
            interface ManagedType {
                $someType.canonicalName getManagedProperty()
            }

            class PluginRules extends RuleSource {
                @Model
                void createModel(ManagedType p) {
                }
            }

            apply plugin: PluginRules

        """

        when:
        fails 'modelReport'

        then:
        failure.assertHasCause("Invalid managed model type ManagedType: read only property 'managedProperty' has non managed type ${someType.name}, only managed types can be used")

        where:
        someType << [
            byte, Byte,
            boolean, Boolean,
            boolean,
            char, Character,
            float, Float,
            long, Long,
            short, Short,
            int, Integer,
            double, Double,
            String,
            BigDecimal,
            BigInteger,
            Configuration.State,
            File]
    }

}
