package me.tatarka.fasax;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Created by evan
 */
@Target(ElementType.TYPE)
public @interface Xml {
    String name() default "";
    Mode mode() default Mode.READWRITE;

    public static enum Mode {
        READWRITE, READ, WRITE
    }
}
