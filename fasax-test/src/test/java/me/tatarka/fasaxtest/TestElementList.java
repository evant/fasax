package me.tatarka.fasaxtest;

import com.google.common.base.Joiner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.tatarka.fasax.Convert;
import me.tatarka.fasax.Element;
import me.tatarka.fasax.ElementList;
import me.tatarka.fasax.Fasax;
import me.tatarka.fasax.Xml;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class TestElementList {
    Fasax fasax;

    @Before
    public void setup() {
        fasax = new Fasax();
    }

    @Test
    public void testConcrete() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <list>",
                "    <item>value1</item>",
                "    <item>value2</item>",
                "  </list>",
                "</root>"
        );
        ConcreteElementList root = fasax.fromXml(xml, ConcreteElementList.class);

        assertThat(root).isNotNull();
        assertThat(root.list).isNotNull();
        assertThat(root.list.get(0)).isEqualTo("value1");
        assertThat(root.list.get(1)).isEqualTo("value2");
    }

    @Test
    public void testAbstract() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <list>",
                "    <item>1</item>",
                "    <item>2</item>",
                "  </list>",
                "</root>"
        );
        AbstractElementList root = fasax.fromXml(xml, AbstractElementList.class);

        assertThat(root).isNotNull();
        assertThat(root.list).isNotNull();
        assertThat(root.list.get(0)).isEqualTo(1);
        assertThat(root.list.get(1)).isEqualTo(2);
    }

    @Test
    public void testDuplicate() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <item>value</item>",
                "  <list>",
                "    <item>1</item>",
                "    <item>2</item>",
                "  </list>",
                "</root>"
        );
        DuplicateElementList root = fasax.fromXml(xml, DuplicateElementList.class);

        assertThat(root).isNotNull();
        assertThat(root.item).isEqualTo("value");
        assertThat(root.list).isNotNull();
        assertThat(root.list.get(0)).isEqualTo(1);
        assertThat(root.list.get(1)).isEqualTo(2);
    }

    @Test
    public void testNested() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <list>",
                "    <child><item>value1</item></child>",
                "    <child><item>value2</item></child>",
                "  </list>",
                "</root>"
        );
        NestedElementList root = fasax.fromXml(xml, NestedElementList.class);

        assertThat(root).isNotNull();
        assertThat(root.list).isNotNull();
        assertThat(root.list.get(0)).isNotNull();
        assertThat(root.list.get(0).item).isEqualTo("value1");
        assertThat(root.list.get(1)).isNotNull();
        assertThat(root.list.get(1).item).isEqualTo("value2");
    }

    @Test
    public void testCustomType() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <list>",
                "    <date>2014-01-01</date>",
                "    <date>2014-01-02</date>",
                "  </list>",
                "</root>"
        );
        CustomTypeElementList root = fasax.fromXml(xml, CustomTypeElementList.class);
        SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-mm-dd");

        assertThat(root).isNotNull();
        assertThat(root.list).isNotNull();
        assertThat(root.list.get(0)).isEqualTo(dateParser.parse("2014-01-01"));
        assertThat(root.list.get(1)).isEqualTo(dateParser.parse("2014-01-02"));
    }

    @Test
    public void testInline() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <item>value1</item>",
                "  <item>value2</item>",
                "</root>"
        );
        InlineElementList root = fasax.fromXml(xml, InlineElementList.class);

        assertThat(root).isNotNull();
        assertThat(root.list).isNotNull();
        assertThat(root.list.get(0)).isEqualTo("value1");
        assertThat(root.list.get(1)).isEqualTo("value2");
    }

    @Test
    public void testInlineCustomType() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <date>2014-01-01</date>",
                "  <date>2014-01-02</date>",
                "</root>"
        );
        InlineCustomTypeElementList root = fasax.fromXml(xml, InlineCustomTypeElementList.class);
        SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-mm-dd");

        assertThat(root).isNotNull();
        assertThat(root.list).isNotNull();
        assertThat(root.list.get(0)).isEqualTo(dateParser.parse("2014-01-01"));
        assertThat(root.list.get(1)).isEqualTo(dateParser.parse("2014-01-02"));
    }

    @Test
    public void testInlineNested() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <child><item>value1</item></child>",
                "  <child><item>value2</item></child>",
                "</root>"
        );
        NestedInlineElementList root = fasax.fromXml(xml, NestedInlineElementList.class);

        assertThat(root).isNotNull();
        assertThat(root.list).isNotNull();
        assertThat(root.list.get(0)).isNotNull();
        assertThat(root.list.get(0).item).isEqualTo("value1");
        assertThat(root.list.get(1)).isNotNull();
        assertThat(root.list.get(1).item).isEqualTo("value2");
    }

    @Xml
    public static class ConcreteElementList {
        @ElementList(entry = "item")
        public ArrayList<String> list;
    }

    @Xml
    public static class AbstractElementList {
        @ElementList(entry = "item")
        public List<Integer> list;
    }

    @Xml
    public static class DuplicateElementList {
        @Element
        public String item;
        @ElementList(entry = "item")
        public List<Integer> list;
    }

    @Xml
    public static class NestedElementList {
        @ElementList(entry = "child")
        public List<NestedElement> list;
    }

    @Xml
    public static class NestedElement {
        @Element
        public String item;
    }

    @Xml
    public static class CustomTypeElementList {
        @Convert(TestFasax.DateConverter.class)
        @ElementList(entry = "date")
        public List<Date> list;
    }

    @Xml
    public static class InlineElementList {
        @ElementList(entry = "item", inline = true)
        public List<String> list;
    }

    @Xml
    public static class InlineCustomTypeElementList {
        @Convert(TestFasax.DateConverter.class)
        @ElementList(entry = "date", inline = true)
        public List<Date> list;
    }

    @Xml
    public static class NestedInlineElementList {
        @ElementList(entry = "child", inline = true)
        public List<NestedElement> list;
    }
}
