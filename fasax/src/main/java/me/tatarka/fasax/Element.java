package me.tatarka.fasax;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Created by evan
 */
@Target(ElementType.FIELD)
public @interface Element {
    String name() default "";
}
