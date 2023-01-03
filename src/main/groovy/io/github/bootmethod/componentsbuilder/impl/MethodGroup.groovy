package io.github.bootmethod.componentsbuilder.impl

import groovy.transform.CompileStatic

import java.lang.reflect.Method

/**
 * @author : WKZ
 * 
 * @created: 2023
 * */
@CompileStatic
class MethodGroup {

    static class MethodSignature {
        Class returnType
        String name
        Class[] parameterTypes

        static MethodSignature valueOf(Method method) {
            return new MethodSignature(returnType: method.returnType, name: method.name, parameterTypes: method.parameterTypes)
        }

        @Override
        boolean equals(Object obj) {
            if (obj == null || !(obj instanceof MethodSignature)) {
                return false
            }
            MethodSignature signature = (MethodSignature) obj
            return Objects.equals(this.returnType, signature.returnType)
                    && Objects.equals(this.name, signature.name)
                    && Arrays.equals(this.parameterTypes, signature.parameterTypes)
        }

        @Override
        int hashCode() {
            return name.hashCode()
        }

    }

    Map<Integer, List<Method>> allMethods = new HashMap<>()

    Map<Integer, List<Method>> methodsNotOverride

    boolean isEmpty() {
        int size = 0
        allMethods.values().each {
            size += it.size()
        }
        return size == 0
    }

    void addMethod(Method method, int group) {
        List<Method> methods = allMethods.get(group)
        if (methods == null) {
            methods = new ArrayList<>()
            allMethods.put(group, methods)
        }
        methods.add(method)
        methodsNotOverride = null
    }

    void invokeMethodsOfGroup(Object instance, int group) {
        this.organize()

        List<Method> methodsWithInThisGroup = methodsNotOverride.get(group)
        if (methodsWithInThisGroup == null) {
            return
        }

        for (Method method : methodsWithInThisGroup) {
            method.invoke(instance)
        }
    }

    private void organize() {
        if (methodsNotOverride) {
            return
        }

        Map<MethodSignature, List<Method>> methodBySignature = new HashMap<>()

        allMethods.values().each { List<Method> list ->
            list.each { Method method ->
                MethodSignature signature = MethodSignature.valueOf(method)
                List<Method> list2 = methodBySignature.get(signature)
                if (list2 == null) {
                    list2 = new ArrayList<>()
                    methodBySignature.put(signature, list2)
                }
                list2.add(method)
            }
        }
        Set<Method> methodsOverride = new HashSet<>()
        //
        methodBySignature.each {
            MethodSignature signature = it.key
            List<Method> methods = it.value//methods with same signature.

            Method theMethod

            methods.each { Method method ->
                Class methodClass = method.getDeclaringClass()
                if (theMethod) {
                    if (theMethod.getDeclaringClass().isAssignableFrom(method.getDeclaringClass())) {
                        theMethod = method
                    }
                } else {
                    theMethod = method
                }
            }

            methods.each {
                if (theMethod != it) {
                    methodsOverride.add(it)
                }
            }
        }
        this.methodsNotOverride = new HashMap<>()
        allMethods.each {
            Integer group = it.key
            List<Method> methods = it.value
            methods.each {
                if (methodsOverride.contains(it)) {
                    return
                }
                List<Method> methods2 = this.methodsNotOverride.get(group)
                if (!methods2) {
                    methods2 = new ArrayList<>()
                    this.methodsNotOverride.put(group, methods2)
                }
                methods2.add(it)

            }
        }

    }

}
