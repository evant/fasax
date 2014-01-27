package me.tatarka.fasaxandroid.model;

import me.tatarka.fasax.Attribute;
import me.tatarka.fasax.Text;
import me.tatarka.fasax.Xml;

@Xml
public class Content {
    @Attribute
    @org.simpleframework.xml.Attribute
    public String type;

    @Text
    @org.simpleframework.xml.Text
    public String value;

    public String getType() {
        return type;
    }

    public void setType(String aType) {
        type = aType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String aValue) {
        value = aValue;
    }
}