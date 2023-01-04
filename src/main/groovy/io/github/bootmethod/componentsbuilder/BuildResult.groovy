package io.github.bootmethod.componentsbuilder

import io.github.bootmethod.componentsbuilder.impl.MapMap

interface BuildResult {
    MapMap<Class, String, Object> getComponents()

    Runnable getDestroyRunner()

}
