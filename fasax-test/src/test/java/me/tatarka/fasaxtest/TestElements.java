package me.tatarka.fasaxtest;

import com.google.common.base.Joiner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.text.SimpleDateFormat;
import java.util.Date;

import me.tatarka.fasax.Convert;
import me.tatarka.fasax.Fasax;
import me.tatarka.fasax.Element;
import me.tatarka.fasax.Xml;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class TestElements {
    Fasax fasax;

    @Before
    public void setup() {
        fasax = new Fasax();
    }

    @Test
    public void testSingle() throws Exception {
        String xml = "<root><item>value</item></root>";
        SingleElement root = fasax.fromXml(xml, SingleElement.class);

        assertThat(root).isNotNull();
        assertThat(root.item).isEqualTo("value");
    }

    @Test
    public void testMissing() throws Exception {
        String xml = "<root/>";
        SingleElement root = fasax.fromXml(xml, SingleElement.class);

        assertThat(root).isNotNull();
        assertThat(root.item).isNull();
    }

    @Test
    public void testExtra() throws Exception {
        String xml = "<root><other>other</other><item>value</item></root>";
        SingleElement root = fasax.fromXml(xml, SingleElement.class);

        assertThat(root).isNotNull();
        assertThat(root.item).isEqualTo("value");
    }

    @Test
    public void testSingleNamed() throws Exception {
        String xml = "<root><item>value</item></root>";
        SingleElementNamed root = fasax.fromXml(xml, SingleElementNamed.class);

        assertThat(root).isNotNull();
        assertThat(root.thing).isEqualTo("value");
    }

    @Test
    public void testPrimitives() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <boolean>true</boolean>",
                "  <int>12</int>",
                "  <float>12.1</float>",
                "  <double>12.22</double>",
                "  <char>c</char>",
                "</root>"
        );
        ElementPrimitives root = fasax.fromXml(xml, ElementPrimitives.class);

        assertThat(root).isNotNull();
        assertThat(root.booleanItem).isTrue();
        assertThat(root.intItem).isEqualTo(12);
        assertThat(root.floatItem).isEqualTo(12.1f);
        assertThat(root.doubleItem).isEqualTo(12.22d);
        assertThat(root.charItem).isEqualTo('c');
    }

    @Test
    public void testCustomType() throws Exception {
        String xml = "<root><date>2014-01-01</date></root>";
        CustomTypeElement root = fasax.fromXml(xml, CustomTypeElement.class);
        SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-mm-dd");

        assertThat(root).isNotNull();
        assertThat(root.date).isEqualTo(dateParser.parse("2014-01-01"));
    }

    @Xml
    public static class SingleElement {
        @Element
        public String item;
    }

    @Xml
    public static class SingleElementNamed {
        @Element(name = "item")
        public String thing;
    }

    @Xml
    public static class ElementPrimitives {
        @Element(name = "boolean")
        public boolean booleanItem;
        @Element(name = "int")
        public int intItem;
        @Element(name = "float")
        public float floatItem;
        @Element(name = "double")
        public double doubleItem;
        @Element(name = "char")
        public char charItem;
    }

    @Xml
    public static class CustomTypeElement {
        @Convert(TestFasax.DateConverter.class)
        @Element
        public Date date;
    }
}
