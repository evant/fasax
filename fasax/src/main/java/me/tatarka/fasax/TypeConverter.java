package me.tatarka.fasax;

import org.xml.sax.SAXException;

public interface TypeConverter<T> {
    T convert(String item) throws SAXException;
}
