package me.tatarka.fasaxtest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.text.SimpleDateFormat;
import java.util.Date;

import me.tatarka.fasax.Fasax;
import me.tatarka.fasax.Attribute;
import me.tatarka.fasax.Xml;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class TestAttributes {
    Fasax fasax;

    @Before
    public void setup() {
        fasax = new Fasax();
    }

    @Test
    public void testSingle() throws Exception {
        String xml = "<root item=\"value\"/>";
        SingleAttribute root = fasax.fromXml(xml, SingleAttribute.class);

        assertThat(root).isNotNull();
        assertThat(root.item).isEqualTo("value");
    }

    @Test
    public void testMissing() throws Exception {
        String xml = "<root/>";
        SingleAttribute root = fasax.fromXml(xml, SingleAttribute.class);

        assertThat(root).isNotNull();
        assertThat(root.item).isNull();
    }

    @Test
    public void testExtra() throws Exception {
        String xml = "<root other=\"other\" item=\"value\"/>";
        SingleAttribute root = fasax.fromXml(xml, SingleAttribute.class);

        assertThat(root).isNotNull();
        assertThat(root.item).isEqualTo("value");
    }

    @Test
    public void testSingleNamed() throws Exception {
        String xml = "<root item=\"value\"/>";
        SingleAttributeNamed root = fasax.fromXml(xml, SingleAttributeNamed.class);

        assertThat(root).isNotNull();
        assertThat(root.thing).isEqualTo("value");
    }

    @Test
    public void testPrimitives() throws Exception {
        String xml =  "<root boolean=\"true\" int=\"12\" float=\"12.1\" double=\"12.22\" char=\"c\"/>";
        AttributePrimitives root = fasax.fromXml(xml, AttributePrimitives.class);

        assertThat(root).isNotNull();
        assertThat(root.booleanItem).isTrue();
        assertThat(root.intItem).isEqualTo(12);
        assertThat(root.floatItem).isEqualTo(12.1f);
        assertThat(root.doubleItem).isEqualTo(12.22d);
        assertThat(root.charItem).isEqualTo('c');
    }

    @Test
    public void testCustomType() throws Exception {
        String xml = "<root date=\"2014-01-01\"/>";
        CustomTypeAttribute root = fasax.fromXml(xml, CustomTypeAttribute.class);
        SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-mm-dd");

        assertThat(root).isNotNull();
        assertThat(root.date).isEqualTo(dateParser.parse("2014-01-01"));
    }

    @Xml(name = "root")
    public static class SingleAttribute {
        @Attribute
        public String item;
    }

    @Xml(name = "root")
    public static class SingleAttributeNamed {
        @Attribute(name = "item")
        public String thing;
    }

    @Xml(name = "root")
    public static class AttributePrimitives {
        @Attribute(name = "boolean")
        public boolean booleanItem;
        @Attribute(name = "int")
        public int intItem;
        @Attribute(name = "float")
        public float floatItem;
        @Attribute(name = "double")
        public double doubleItem;
        @Attribute(name = "char")
        public char charItem;
    }

    @Xml(name = "root")
    public static class CustomTypeAttribute {
        @Attribute(type = TestFasax.DateTypeConverter.class)
        public Date date;
    }
}
