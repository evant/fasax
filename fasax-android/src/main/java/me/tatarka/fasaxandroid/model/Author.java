package me.tatarka.fasaxandroid.model;

import me.tatarka.fasax.Element;
import me.tatarka.fasax.Xml;

@Xml
public class Author {
    @Element
    @org.simpleframework.xml.Element
	public String name;
	
    @Element
    @org.simpleframework.xml.Element
	public String uri;

    public String getName() {
        return name;
    }

    public void setName(String aName) {
        name = aName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String aUri) {
        uri = aUri;
    }
}
