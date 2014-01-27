package me.tatarka.fasaxtest;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xml.sax.SAXException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.tatarka.fasax.Converter;
import me.tatarka.fasax.Fasax;
import me.tatarka.fasax.Xml;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class TestFasax {
    Fasax fasax;

    @Before
    public void setup() {
        fasax = new Fasax();
    }

    @Test
    public void testEmpty() throws Exception {
        String xml = "<root/>";
        Empty root = fasax.fromXml(xml, Empty.class);

        assertThat(root).isNotNull();
    }

    @Test(expected = SAXException.class)
    public void testInvalid() throws Exception {
        String xml = "invalid xml";
        fasax.fromXml(xml, Empty.class);
    }

    @Xml
    public static class Empty {

    }

    public static class DateConverter implements Converter<Date> {
        SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-mm-dd");

        @Override
        public Date read(String item) throws SAXException {
            try {
                return dateParser.parse(item);
            } catch (ParseException e) {
                throw new SAXException(e);
            }
        }
    }
}
