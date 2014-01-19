package me.tatarka.fasax;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Created by evan
 */
@Target(ElementType.FIELD)
public @interface ElementList {
    String entry() default "";
    String name() default "";
    Class type() default void.class;
    boolean inline() default false;
}
