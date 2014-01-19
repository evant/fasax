package me.tatarka.fasax.internal;

import java.io.IOException;
import java.io.Writer;

public abstract class AdolpheXmlWriter<T> {
    public abstract void write(T item, Writer w) throws IOException;
}
