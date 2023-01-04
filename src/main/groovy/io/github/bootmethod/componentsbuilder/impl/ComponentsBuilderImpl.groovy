package io.github.bootmethod.componentsbuilder.impl

import groovy.transform.CompileStatic
import io.github.bootmethod.componentsbuilder.BuildResult
import io.github.bootmethod.componentsbuilder.Components
import io.github.bootmethod.componentsbuilder.ComponentsBuilder
import io.github.bootmethod.componentsbuilder.annotation.Component
import io.github.bootmethod.componentsbuilder.annotation.Destroyer
import io.github.bootmethod.componentsbuilder.annotation.Initializer
import io.github.bootmethod.componentsbuilder.annotation.Variable

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

/**
 * @author : WKZ
 *
 * @created: 2023
 * */
@CompileStatic
class ComponentsBuilderImpl implements ComponentsBuilder {

    static Runnable NULL_RUNNER = {}

    static class BuildResultImpl implements BuildResult {
        MapMap<Class, String, Object> components
        Runnable destroyRunner
    }

    private static class ServiceRelation {
        InstanceHolder instanceHolder
        Field field
        Component component
        Variable variable
    }

    private static class InstanceHolder {
        Class type
        String name
        Object instance
        Class implClass

        MethodGroup initializeMethods = new MethodGroup()
        MethodGroup destroyMethods = new MethodGroup()
        Set<Method> invokedMethod = new HashSet<>()

        Object invoke(Method method, Object... args) {
            if (invokedMethod.contains(method)) {
                throw new RuntimeException("cannot invoke second time of method:${implClass.getName()}#${method.getName()}")
            }
            Object obj = method.invoke(instance, args)
            invokedMethod.add(method)
            return obj
        }
    }

    MapMap<Class, String, Supplier> rootSuppliers = new MapMap<>()

    MapMap<Class, String, Object> components = new MapMap<>()

    Map<String, Object> variables = new HashMap<>()

    /**
     * if type is null, the second map will be bound as variables
     * @param context
     * @return
     */
    @Override
    ComponentsBuilderImpl bindAllVariables(Map<String, Object> variables) {
        variables.each {
            bind(it.key, it.value)
        }
        return this
    }

    @Override
    ComponentsBuilderImpl bindAll(Map<Class, Map<String, Object>> context) {
        MapMap<Class, String, Object> map = MapMap.valueOf(context)
        map.eachEntry({ Class type, String name, Object value ->
            this.bind(type, name, value)
        } as Consumer3<Class, String, Object>)
        return this
    }

    @Override
    ComponentsBuilderImpl bindAllComponents(Map<Class, Object> map) {
        map.each {
            bind(it.key, it.value)
        }
        return this
    }

    @Override
    ComponentsBuilderImpl bind(Class type, Object value) {
        return bind(type, null, value)
    }

    @Override
    ComponentsBuilderImpl bind(String name, Object value) {
        return bind(null, name, value)
    }

    @Override
    ComponentsBuilderImpl bind(Class type, String name, Object value) {
        if (type) {
            this.components.put(type, name, value)
        } else {
            this.variables.put(name, value)
        }
        return this
    }

    @Override
    ComponentsBuilderImpl addAll(Class... implClasses) {
        Map<Class, Class> map = new HashMap<>()
        implClasses.each {
            map.put(it, it)
        }
        return addAll(map)
    }

    @Override
    ComponentsBuilderImpl addAll(Map<Class, Class> serviceSpecs) {
        serviceSpecs.each {
            add(it.key, it.value)
        }
        return this
    }

    @Override
    ComponentsBuilderImpl add(Class type, Class implClass) {
        return add(type, {
            return implClass.getConstructor().newInstance()
        } as Supplier)
    }

    @Override
    ComponentsBuilderImpl add(Class type, Supplier instanceFactory) {
        this.rootSuppliers.put(type, null, instanceFactory)
        return this
    }

    @Override
    BuildResultImpl build() {
        MapMap<Class, String, InstanceHolder> instanceHolderMap = doBuild()
        //Transform to instance map
        MapMap<Class, String, Object> instanceMap = instanceHolderMap.transform({
            it.instance
        })

        List<InstanceHolder> instanceHasDestroyMethods = new ArrayList<>()

        instanceHolderMap.eachValue({ InstanceHolder holder ->
            if (!holder.destroyMethods.isEmpty()) {
                instanceHasDestroyMethods.add(holder)
            }
        })

        Runnable destroyRunner = instanceHasDestroyMethods.isEmpty() ? NULL_RUNNER :
                {
                    //todo: sort the method calling.
                    instanceHasDestroyMethods.each { InstanceHolder instanceHolder ->
                        instanceHolder.destroyMethods.invokeMethodsOfGroup(instanceHolder.instance, 0)
                    }
                } as Runnable

        return new BuildResultImpl(components: instanceMap, destroyRunner: destroyRunner)
    }

    @Override
    Object build(Class type) {
        BuildResultImpl result = this.build()
        Object instance = result.getComponents().get(type, null)
        if (instance == null) {
            throw new RuntimeException("no such component with type:${type.getName()}")
        }
        if (result.destroyRunner != NULL_RUNNER) {
            throw new RuntimeException("cannot build single component, destroy methods found.")
        }
        return instance
    }

    @Override
    Components buildComponents() {
        return build(new ComponentsImpl())
    }

    @Override
    Components bindAndBuild(Components components) {
        return this.bindAll(components.getAsMap()).build(components)
    }

    @Override
    Components build(Components components) {
        BuildResultImpl result = this.build()
        components.setComponents(result.components.map)
        if (result.destroyRunner != NULL_RUNNER) {
            components.addDestroyHook(result.destroyRunner)
        }
        return components
    }

    private MapMap<Class, String, InstanceHolder> doBuild() {

        //Create instances for each type.
        List<ServiceRelation> relationList = new ArrayList<>()
        MapMap<Class, String, Supplier> allServiceSpecs = new MapMap<>(this.rootSuppliers)
        MapMap<Class, String, InstanceHolder> instanceHolderMap = populateInstances(0, allServiceSpecs, new MapMap<>(), relationList)

        //Resolve dependency
        relationList.each {
            Field field = it.field
            Class type = it.field.getType()
            Object instance = it.instanceHolder.instance
            Object instance2 = null

            if (it.component != null) {
                Object instanceHolder2 = instanceHolderMap.get(type, null)
                if (instanceHolder2 == null) {//target object is in context components or variables.
                    instance2 = components.get(type, null)
                    if (instance2 == null) {
                        throw new RuntimeException("Cannot resolve the instance to inject field : ${type.getName()} ${field.getDeclaringClass().getName()}#${field.getName()}")
                    }
                } else {// target object is created locally.
                    instance2 = instanceHolder2.instance
                }
            } else if (it.variable != null) {
                String varName = it.variable.name()
                if (varName == null || varName.length() == 0) {
                    varName = field.getName()
                }
                instance2 = variables.get(varName)
                if (instance2 == null && it.variable.required()) {
                    throw new RuntimeException("Cannot resolve the instance to inject fields: ${type.getName()} ${field.getDeclaringClass().getName()}#${field.getName()}")
                }
            } else {
                throw new RuntimeException("Cannot resolve the instance to inject fields: ${type.getName()} ${field.getDeclaringClass().getName()}#${field.getName()}")
            }

            it.field.setAccessible(true)
            it.field.set(instance, instance2)
        }
        AtomicInteger minGroupForInitializer = new AtomicInteger(Integer.MAX_VALUE)
        AtomicInteger maxGroupForInitializer = new AtomicInteger(Integer.MIN_VALUE)
        AtomicInteger minGroupForDestroyer = new AtomicInteger(Integer.MAX_VALUE)
        AtomicInteger maxGroupForDestroyer = new AtomicInteger(Integer.MIN_VALUE)

        //Collect initialize methods
        instanceHolderMap.eachValue {
            resolveInitializeMethod(it, minGroupForInitializer, maxGroupForInitializer)
            resolveDestroyMethod(it, minGroupForDestroyer, maxGroupForDestroyer)
        }

        //Invoke initialize methods by group
        for (int i = minGroupForInitializer.get(); i <= maxGroupForInitializer.get(); i++) {
            instanceHolderMap.eachValue { InstanceHolder instanceHolder ->
                instanceHolder.initializeMethods.invokeMethodsOfGroup(instanceHolder.instance, i)
            }
        }

        return instanceHolderMap
    }

    static private MapMap<Class, String, InstanceHolder> populateInstances(int depth, MapMap<Class, String, Supplier> serviceSpecs,
                                                                           MapMap<Class, String, InstanceHolder> instanceHolderMap,
                                                                           List<ServiceRelation> relationList) {


        MapMap<Class, String, InstanceHolder> generatedInstances = new MapMap<>()
        serviceSpecs.eachEntry({ Class type, String name, Supplier supplier ->

            Object instance = supplier.get()
            Class implClass = instance.getClass()

            if (instanceHolderMap.containsKey(type, name)) {
                throw new RuntimeException("duplicated impl with type:${type} and name:${name}")
            }

            InstanceHolder instanceHolder = new InstanceHolder(type: type, instance: instance, implClass: implClass)
            instanceHolderMap.put(type, name, instanceHolder)

            generatedInstances.put(type, name, instanceHolder)

            Class theClass = instance.getClass()
            while (theClass != Object.class) {
                //relation of instance
                theClass.getDeclaredFields().each { Field field ->
                    Component component = field.getAnnotation(Component.class)
                    Variable variable = field.getAnnotation(Variable.class)

                    if (component != null || variable != null) {

                        ServiceRelation serviceRelation = new ServiceRelation(instanceHolder: instanceHolder,
                                field: field,
                                component: component,
                                variable: variable)

                        relationList.add(serviceRelation)
                    }
                }
                theClass = theClass.getSuperclass()
            }
            //
        } as Consumer3<Class, String, Supplier>)

        //Resolve additional specs in case the newly generated instance has more spec declared.
        MapMap<Class, String, Supplier> generatedSpecs = new MapMap<>()
        generatedInstances.eachValue({ InstanceHolder holder ->
            resolveSpecify(holder, generatedSpecs)
        })

        if (!generatedSpecs.isEmpty()) {
            //Populate instance recursively on the folder of services.
            populateInstances(depth + 1, generatedSpecs, instanceHolderMap, relationList)
        }
        return instanceHolderMap
    }

    static Object invokeSpecifyMethod(InstanceHolder holder, Method method, Object... args) {
        if (Modifier.isStatic(method.getModifiers())) {
            throw new RuntimeException("static method is not supported as the spec method")
        }
        return holder.invoke(method, args)
    }

    static void resolveSpecify(InstanceHolder holder, MapMap<Class, String, Supplier> generatedSpecs) {

        Class cls = holder.instance.getClass()
        while (cls != Object.class) {
            cls.getDeclaredMethods().each { Method method ->
                io.github.bootmethod.componentsbuilder.annotation.Factory specify = method.getAnnotation(io.github.bootmethod.componentsbuilder.annotation.Factory.class)
                if (!specify) {
                    return
                }

                Class returnType = method.getReturnType()
                Class[] parameterTypes = method.getParameterTypes()

                Map<Class, Object> specMap = new HashMap<>()
                if (returnType == Class.class as Class) {
                    if (parameterTypes.length > 0) {
                        throw methodSignatureNotSupport(method)
                    }
                    Class componentCls = invokeSpecifyMethod(holder, method) as Class
                    specMap.put(componentCls, componentCls)
                } else if (returnType == Object.class) {
                    if (parameterTypes.length > 0) {
                        throw methodSignatureNotSupport(method)
                    }
                    Object obj = invokeSpecifyMethod(holder, method)
                    specMap.put(obj.getClass(), obj)

                } else if (returnType == Map.class) {
                    if (parameterTypes.length == 0) {
                        Map map = invokeSpecifyMethod(holder, method) as Map
                        specMap.putAll(map)
                    } else if (parameterTypes.length == 1) {
                        if (parameterTypes[0] != Map.class) {
                            throw methodSignatureNotSupport(method)
                        }
                        Object obj = invokeSpecifyMethod(holder, method, new HashMap())
                        Map map = obj as Map
                        specMap.putAll(map)
                    } else {

                        throw methodSignatureNotSupport(method)
                    }
                } else if (returnType == void.class) {
                    if (parameterTypes.length == 1) {
                        if (parameterTypes[0] != Map.class) {
                            throw methodSignatureNotSupport(method)
                        }
                        Map map = new HashMap()
                        invokeSpecifyMethod(holder, method, map)
                        specMap.putAll(map)
                    } else {
                        throw methodSignatureNotSupport(method)
                    }

                } else {
                    throw methodSignatureNotSupport(method)
                }

                specMap.each {
                    Class type = it.key
                    Object obj = it.value
                    if (type.isInstance(obj)) {
                        generatedSpecs.put(type, null, { obj } as Supplier)
                    } else if (obj.getClass() == Class.class) {
                        Class componentCls = (Class) obj
                        Supplier supplier = { componentCls.getConstructor().newInstance() } as Supplier
                        generatedSpecs.put(type, null, supplier)
                    } else if (obj.getClass() == Supplier.class) {
                        generatedSpecs.put(type, null, obj as Supplier)
                    } else {
                        throw new RuntimeException("not supported value type:${obj},key:${type}, method signature:${method.getDeclaringClass().getName()}#${method.getName()}")
                    }
                }

            }

            cls = cls.getSuperclass()
        }

    }

    RuntimeException methodSignatureNotSupport(Method method) {
        new RuntimeException("not supported method signature:${method.getDeclaringClass().getName()}#${method.getName()}")
    }


    static Range<Integer> resolveInitializeMethod(InstanceHolder instanceHolder, AtomicInteger minGroup, AtomicInteger maxGroup) {
        int localMinGroup = Integer.MAX_VALUE
        int localMaxGroup = Integer.MIN_VALUE
        Class theClass = instanceHolder.implClass
        while (theClass != Object.class) {
            theClass.getDeclaredMethods().each { Method method ->
                Initializer initializeAno = method.getAnnotation(Initializer.class)
                if (!initializeAno) {
                    //not the initializer method
                    return
                }

                if (method.getParameterTypes().size() != 0) {
                    throw new RuntimeException("initialize method(${method}) signature error, should be no parameters.")
                }
                int group = initializeAno.group()
                if (group > localMaxGroup) {
                    localMaxGroup = group
                }
                if (group < localMinGroup) {
                    localMinGroup = group
                }

                if (group > maxGroup.get()) {
                    maxGroup.set(group)
                }
                if (group < minGroup.get()) {
                    minGroup.set(group)
                }
                instanceHolder.initializeMethods.addMethod(method, group)
            }
            theClass = theClass.getSuperclass()
        }

        return localMinGroup..localMaxGroup
    }

    static Range<Integer> resolveDestroyMethod(InstanceHolder instanceHolder, AtomicInteger minGroup, AtomicInteger maxGroup) {
        int localMaxGroup = Integer.MIN_VALUE
        int localMinGroup = Integer.MAX_VALUE

        Class theClass = instanceHolder.implClass
        while (theClass != Object.class) {
            theClass.getDeclaredMethods().each { Method method ->
                Destroyer destroyerAno = method.getAnnotation(Destroyer.class)
                if (!destroyerAno) {
                    //not the destroyer method
                    return
                }

                if (method.getParameterTypes().size() != 0) {
                    throw new RuntimeException("destroy method(${method}) signature error, should be no parameters.")
                }
                int group = destroyerAno.group()
                if (group > localMaxGroup) {
                    localMaxGroup = group
                }

                if (group < localMinGroup) {
                    localMinGroup = group
                }

                if (group > maxGroup.get()) {
                    maxGroup.set(group)
                }

                if (group < minGroup) {
                    minGroup.set(group)
                }

                instanceHolder.destroyMethods.addMethod(method, group)

            }
            theClass = theClass.getSuperclass()
        }

        return localMinGroup..localMaxGroup
    }
}
