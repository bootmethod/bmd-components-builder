package io.github.bootmethod.componentsbuilder

import io.github.bootmethod.componentsbuilder.impl.ComponentsBuilderImpl

import java.util.function.Supplier

interface ComponentsBuilder {
    static class Factory {
        static ComponentsBuilder create() {
            return new ComponentsBuilderImpl()
        }
    }

    ComponentsBuilder bindAllVariables(Map<String, Object> variables)

    ComponentsBuilder bindAll(Map<Class, Map<String, Object>> context)

    ComponentsBuilder bindAllComponents(Map<Class, Object> map)

    ComponentsBuilder bind(Class type, Object value)

    ComponentsBuilder bind(String name, Object value)

    ComponentsBuilder bind(Class type, String name, Object value)

    ComponentsBuilder addAll(Class... implClasses)

    ComponentsBuilder addAll(Map<Class, Class> serviceSpecs)

    ComponentsBuilder add(Class type, Class implClass)

    ComponentsBuilder add(Class type, Supplier instanceFactory)

    BuildResult build()

    Object build(Class type)

    Components buildComponents()

    Components bindAndBuild(Components components)

    Components build(Components components)

}
