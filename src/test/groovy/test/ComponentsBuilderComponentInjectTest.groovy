package test

import io.github.bootmethod.componentsbuilder.ComponentsBuilder
import io.github.bootmethod.componentsbuilder.annotation.Component
import spock.lang.Specification

class ComponentsBuilderComponentInjectTest extends Specification {
    static interface Component1 {
        String doSomeThing()
    }

    static interface Component2 {
        String doSomeThing()
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

        @Override
        String doSomeThing() {
            component1.doSomeThing()
        }
    }

    def "test-components-builder-simple1"() {
        ComponentsBuilder builder = ComponentsBuilder.Factory.create()
        builder.add(Component1.class, Component1Impl.class)
        builder.add(Component2.class, Component2Impl.class)
        Component2 myComponent2 = builder.build(Component2.class)
        String message = myComponent2.doSomeThing()
        expect:
        message == "OK"

    }

}
