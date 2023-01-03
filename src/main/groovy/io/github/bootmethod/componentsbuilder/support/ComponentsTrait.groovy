package io.github.bootmethod.componentsbuilder.support


import groovy.transform.CompileStatic
import io.github.bootmethod.componentsbuilder.Components

/**
 * @author : WKZ
 * 
 * @created: 2023
 * */
@CompileStatic
trait ComponentsTrait implements Components {


    Map<Class, Map<String, Object>> components = new HashMap<>()

    List<Runnable> destroyHooks = new ArrayList<>()

    @Override
    Map<Class, Map<String, Object>> getAsMap() {
        return components
    }

    @Override
    Object getComponent(Class<?> type) {
        return getComponent(type, false)
    }

    @Override
    Object getComponent(Class<?> type, boolean force) {
        return getComponent(type, null, force)
    }

    @Override
    void setComponent(Class type, Object obj) {
        this.setComponent(type, null, obj)
    }

    @Override
    void setComponents(Map<Class, Map<String, Object>> components) {
        components.each {
            Class type = it.key
            it.value.each { Map.Entry<String, Object> entry ->
                setComponent(type, entry.key, entry.value)
            }
        }
    }

    @Override
    Object getComponent(Class type, String name) {
        return getComponent(type, name, false)
    }

    @Override
    Object getComponent(Class type, String name, boolean force) {
        Map<String, Object> map = components.get(type)
        Object obj
        if (map) {
            obj = map.get(name)
        }
        if (force && obj == null) {
            throw new RuntimeException("no component found by type:${type.getName()} and name:${name}")
        }
        return obj
    }

    @Override
    void setComponent(Class type, String name, Object obj) {
        if (!type.isInstance(obj)) {
            throw new IllegalArgumentException("object is not a instance of type:${type.getName()}, name:${name ?: ""}, obj:${obj}")
        }
        Map<String, Object> map = this.components.get(type)
        if (!map) {
            map = new HashMap<>()
            components.put(type, map)
        }
        Object oldObj = map.get(name)

        if (oldObj) {
            if (oldObj == obj) {
                //WARN
                return
            }
            throw new RuntimeException("duplicate component with type:${type} and name:${name}")
        }

        map.put(name, obj)
    }

    @Override
    List findComponents(Class type) {
        List list = new ArrayList<>()
        components.values().each({
            Class cls = it.key
            it.each {
                Object obj = it.value
                if (type.isInstance(cls)) {
                    list.add(obj)
                }
            }
        })
        return list
    }

    @Override
    void addDestroyHook(Runnable runnable) {
        this.destroyHooks.add(runnable)
    }

    @Override
    void destroy() {
        for (Runnable runnable : this.destroyHooks) {
            runnable.run()
        }
        this.components.clear()
    }
}
