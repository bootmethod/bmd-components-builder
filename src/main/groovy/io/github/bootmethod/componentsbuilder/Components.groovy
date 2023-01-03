package io.github.bootmethod.componentsbuilder
/**
 * @author : WKZ
 * 
 * @created: 2023
 * */

interface Components {

    Object getComponent(Class<?> type)

    Object getComponent(Class<?> type, boolean force)

    void setComponent(Class<?> type, Object obj)

    Map<Class, Map<String, Object>> getAsMap()

    void setComponents(Map<Class, Map<String, Object>> components)

    Object getComponent(Class type, String name)

    Object getComponent(Class type, String name, boolean force)

    void setComponent(Class type, String name, Object obj)

    List findComponents(Class type)

    void addDestroyHook(Runnable runnable)

    void destroy()
}
