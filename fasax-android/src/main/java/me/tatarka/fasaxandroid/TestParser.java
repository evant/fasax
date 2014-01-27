package me.tatarka.fasaxandroid;

public class TestParser<T> {
    public final String name;
    public final Parser<T> parser;
    public long time = 0;
    public int percent = 0;

    public TestParser(String name, Parser<T> parser) {
        this.name = name;
        this.parser = parser;
    }
}
