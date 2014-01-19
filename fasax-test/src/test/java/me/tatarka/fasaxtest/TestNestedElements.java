package me.tatarka.fasaxtest;

import com.google.common.base.Joiner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import me.tatarka.fasax.Fasax;
import me.tatarka.fasax.Element;
import me.tatarka.fasax.Xml;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class TestNestedElements {
    Fasax fasax;

    @Before
    public void setup() {
        fasax = new Fasax();
    }

    @Test
    public void testSingleNested() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <inner>",
                "    <item>value</item>",
                "  </inner>",
                "</root>"
        );

        SingleNested root = fasax.fromXml(xml, SingleNested.class);

        assertThat(root).isNotNull();
        assertThat(root.inner).isNotNull();
        assertThat(root.inner.item).isEqualTo("value");
    }

    @Test
    public void testNestedMissing() throws Exception {
        String xml = "<root/>";

        SingleNested root = fasax.fromXml(xml, SingleNested.class);

        assertThat(root).isNotNull();
        assertThat(root.inner).isNull();
    }

    @Test
    public void testNestedDuplicateName() throws Exception {
        String xml = Joiner.on("\n").join(
                "<root>",
                "  <item>value</item>",
                "  <inner>",
                "    <item>value</item>",
                "  </inner>",
                "</root>"
        );

        SingleNestedDuplicate root = fasax.fromXml(xml, SingleNestedDuplicate.class);

        assertThat(root).isNotNull();
        assertThat(root.item).isEqualTo("value");
        assertThat(root.inner).isNotNull();
        assertThat(root.inner.item).isEqualTo("value");
    }

    @Xml(name = "root")
    public static class SingleNested {
        @Element
        public Inner inner;
    }

    @Xml
    public static class Inner {
        @Element
        public String item;
    }

    @Xml
    public static class SingleNestedDuplicate {
        @Element
        public String item;

        @Element
        public Inner inner;
    }
}
