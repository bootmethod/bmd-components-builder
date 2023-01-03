package io.github.bootmethod.componentsbuilder.impl

import groovy.transform.CompileStatic

/**
 * @author : WKZ
 * 
 * @created: 2023
 * */
@CompileStatic
@FunctionalInterface
interface Consumer3<V1, V2, V3> {
    void accept(V1 v1, V2 v2, V3 v3)
}
