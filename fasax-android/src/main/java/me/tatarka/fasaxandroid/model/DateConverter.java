package me.tatarka.fasaxandroid.model;

import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.xml.sax.SAXException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.tatarka.fasax.Converter;

public class DateConverter implements Converter<Date>, org.simpleframework.xml.convert.Converter<Date> {
    SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public Date read(String item) throws SAXException {
        try {
            return dateParser.parse(item);
        } catch (ParseException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public Date read(InputNode node) throws Exception {
        return read(node.getValue());
    }

    @Override
    public void write(OutputNode outputNode, Date date) throws Exception {

    }
}

