package me.tatarka.fasax;

/**
 * Created by evan
 */
public @interface Convert {
    Class<? extends Converter> value();
}
