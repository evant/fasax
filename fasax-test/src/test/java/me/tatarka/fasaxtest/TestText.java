package me.tatarka.fasaxtest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.text.SimpleDateFormat;
import java.util.Date;

import me.tatarka.fasax.Fasax;
import me.tatarka.fasax.Attribute;
import me.tatarka.fasax.Text;
import me.tatarka.fasax.Xml;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class TestText {
    Fasax fasax;

    @Before
    public void setup() {
        fasax = new Fasax();
    }

    @Test
    @Ignore
    public void testSingle() throws Exception {
        String xml = "<root>text</root>";
        SingleText root = fasax.fromXml(xml, SingleText.class);

        assertThat(root).isNotNull();
        assertThat(root.text).isEqualTo("text");
    }

    @Test
    public void testMissing() throws Exception {
        String xml = "<root/>";
        SingleText root = fasax.fromXml(xml, SingleText.class);

        assertThat(root).isNotNull();
        assertThat(root.text).isNull();
    }

    @Test
    @Ignore
    public void testWithAttribute() throws Exception {
        String xml = "<root item=\"value\">text</root>";
        SingleTextWithAttribute root = fasax.fromXml(xml, SingleTextWithAttribute.class);

        assertThat(root).isNotNull();
        assertThat(root.item).isEqualTo("value");
        assertThat(root.text).isEqualTo("text");
    }

    @Xml(name = "root")
    public static class SingleText {
        @Text
        public String text;
    }

    @Xml(name = "root")
    public static class SingleTextWithAttribute {
        @Attribute
        public String item;
        @Text
        public String text;
    }
}

