package me.tatarka.fasax;

import org.xml.sax.SAXException;

public interface Converter<T> {
    T read(String item) throws SAXException;
}
