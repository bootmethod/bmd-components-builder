package test

import io.github.bootmethod.componentsbuilder.ComponentsBuilder
import io.github.bootmethod.componentsbuilder.annotation.Component
import io.github.bootmethod.componentsbuilder.annotation.Variable
import spock.lang.Specification

class ComponentsBuilderVarTest extends Specification {

    static interface Component2 {
        String doSomeThing()
    }

    static class Component2Impl implements Component2 {
        @Variable
        String var1

        @Override
        String doSomeThing() {
            return var1
        }
    }

    def "test-components-builder-var1"() {
        ComponentsBuilder builder = ComponentsBuilder.Factory.create()
        builder.bind("var1", "OK")
        builder.add(Component2.class, Component2Impl.class)
        Component2 myComponent2 = builder.build(Component2.class)
        expect:
        "OK" == myComponent2.doSomeThing()

    }

}
