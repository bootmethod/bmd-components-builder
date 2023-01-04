package test

import io.github.bootmethod.componentsbuilder.ComponentsBuilder
import io.github.bootmethod.componentsbuilder.annotation.Component
import io.github.bootmethod.componentsbuilder.annotation.Initializer
import spock.lang.Specification

class ComponentsBuilderInitializerTest extends Specification {
    static interface Component1 {
        String doSomeThing()
    }

    static interface Component2 {
        String doSomeThing()

        boolean isInitialized()
    }

    static class Component1Impl implements Component1 {

        @Override
        String doSomeThing() {
            return "OK"
        }
    }

    static class Component2Impl implements Component2 {
        @Component
        Component1 component1

        boolean initialized

        @Initializer
        void init() {
            initialized = true
        }

        @Override
        String doSomeThing() {
            component1.doSomeThing()
        }
    }

    def "test-components-builder-initializer"() {
        ComponentsBuilder builder = ComponentsBuilder.Factory.create()
        builder.add(Component1.class, Component1Impl.class)
        builder.add(Component2.class, Component2Impl.class)
        Component2 myComponent2 = builder.build(Component2.class)
        expect:
        true == myComponent2.isInitialized()
        "OK" == myComponent2.doSomeThing()

    }

}
