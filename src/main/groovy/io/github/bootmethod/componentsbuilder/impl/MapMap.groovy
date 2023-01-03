package io.github.bootmethod.componentsbuilder.impl

import groovy.transform.CompileStatic

import java.util.function.Consumer
import java.util.function.Function

/**
 * @author : WKZ
 * 
 * @created: 2023
 * */
@CompileStatic
class MapMap<K1, K2, V> {

    Map<K1, Map<K2, V>> map = new HashMap<>()

    MapMap() {
    }

    static <K1,K2,V> MapMap valueOf(Map<K1, Map<K2, V>> map) {
        MapMap<K1, K2, V> mapMap = new MapMap<K1, K2, V>()
        doTransform(map, { V v -> v }, mapMap.map)
        return mapMap
    }

    MapMap(MapMap<K1, K2, V> map) {
        doTransform(map.map, { V v -> v }, this.map)
    }

    V get(K1 k1, K2 k2) {
        Map<K2, V> map2 = map.get(k1)
        if (map2) {
            return map2.get(k2)
        }
        return null
    }

    def V put(K1 k1, K2 k2, V value) {
        if (value instanceof HashMap) {
            throw new RuntimeException()
        }
        Map<K2, V> map2 = map.get(k1)
        if (map2 == null) {
            map2 = new HashMap<>()
            map.put(k1, map2)
        }
        return map2.put(k2, value)
    }

    def void eachValue(Consumer<V> consumer) {
        map.each {
            it.value.each { Map.Entry<K2, V> entry ->
                consumer.accept(entry.value)
            }
        }
    }

    def void eachEntry(Consumer3<K1, K2, V> consumer) {
        map.each {
            K1 k1 = it.key
            it.value.each {
                K2 k2 = it.key
                V v = it.value
                consumer.accept(k1, k2, v)
            }
        }
    }

    def <V2> MapMap<K1, K2, V2> transform(Function<V, V2> transform) {

        MapMap map = new MapMap()
        doTransform(this.map, transform, map.map)
        return map
    }

    private static <K1, K2, V1, V2> Map<K1, Map<K2, V2>> doTransform(Map<K1, Map<K2, V1>> map, Function<V1, V2> transform, Map<K1, Map<K2, V2>> map2) {

        map.each {
            K1 k1 = it.key
            it.value.each {
                K2 k2 = it.key
                V1 v1 = it.value
                Map<K2, V2> map22 = map2.get(it.key)
                if (map22 == null) {
                    map22 = new HashMap<>()
                    map2.put(k1, map22)
                }
                map22.put(k2, transform.apply(v1))
            }
        }
        return map2
    }

    def boolean containsKey(K1 k1, K2 k2) {
        Map<K2, V> map2 = map.get(k1)
        if (map2 == null) {
            return false
        }
        return map2.containsKey(k2)
    }

    boolean isEmpty() {
        return map.isEmpty()
    }
}
