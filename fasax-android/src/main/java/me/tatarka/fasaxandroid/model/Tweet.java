package me.tatarka.fasaxandroid.model;

import org.simpleframework.xml.Root;

import java.util.Date;

import me.tatarka.fasax.Convert;
import me.tatarka.fasax.Element;
import me.tatarka.fasax.Xml;

@Xml
@Root(name = "entry", strict = false)
public class Tweet {
    @Element
    @org.simpleframework.xml.Element
	public String title;

    @Convert(DateConverter.class)
    @Element
    @org.simpleframework.xml.Element
	public Date published;

    @Element
    @org.simpleframework.xml.Element
	public Content content;

    @Element(name="lang")
    @org.simpleframework.xml.Element(name = "lang")
	public String language;

    @Element
    @org.simpleframework.xml.Element
	public Author author;

    public String getTitle() {
        return title;
    }

    public void setTitle(String aTitle) {
        title = aTitle;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date aDate) {
        published = aDate;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content aContent) {
        content = aContent;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String aLanguage) {
        language = aLanguage;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author aAuthor) {
        author = aAuthor;
    }
}
