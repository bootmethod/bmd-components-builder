package io.github.bootmethod.componentsbuilder.annotation


import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * @author : WKZ
 * 
 * @created: 2023
 * */

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD])
@interface Factory {

}
