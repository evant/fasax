package me.tatarka.fasaxandroid;

import java.io.InputStream;

public interface Parser<T> {
    T parse(InputStream in) throws Exception;
}
