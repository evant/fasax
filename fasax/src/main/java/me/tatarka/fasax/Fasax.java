package me.tatarka.fasax;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import me.tatarka.fasax.internal.FasaxHandler;
import me.tatarka.fasax.internal.AdolpheXmlWriter;
import me.tatarka.fasax.internal.FasaxReaderGenerator;
import me.tatarka.fasax.internal.FasaxWriterGenerator;

public class Fasax {
    private final Map<Class<?>, FasaxHandler> SAX_HANDLERS = new LinkedHashMap<Class<?>, FasaxHandler>();
    private final Map<Class<?>, AdolpheXmlWriter>  XML_WRITERS = new LinkedHashMap<Class<?>, AdolpheXmlWriter>();

    private SAXParserFactory factory;

    public <T> T fromXml(String xml, Class<T> clazz) throws IOException, SAXException {
        FasaxHandler<T> handler = findSaxHandler(clazz);
        SAXParser parser = newParser();
        parser.parse(new InputSource(new StringReader(xml)), handler);
        return handler.getResult();
    }

    public <T> T fromXml(InputStream in, Class<T> clazz) throws IOException, SAXException {
        FasaxHandler<T> handler = findSaxHandler(clazz);
        SAXParser parser = newParser();
        parser.parse(in, handler);
        return handler.getResult();
    }

    public <T> T fromXml(InputSource is, Class<T> clazz) throws IOException, SAXException {
        FasaxHandler<T> handler = findSaxHandler(clazz);
        SAXParser parser = newParser();
        parser.parse(is, handler);
        return handler.getResult();
    }

    public <T> T fromXml(File file, Class<T> clazz) throws IOException, SAXException {
        FasaxHandler<T> handler = findSaxHandler(clazz);
        SAXParser parser = newParser();
        parser.parse(file, handler);
        return handler.getResult();
    }

    public <T> T fromXml(Reader reader, Class<T> clazz) throws IOException, SAXException {
        FasaxHandler<T> handler = findSaxHandler(clazz);
        SAXParser parser = newParser();
        parser.parse(new InputSource(reader), handler);
        return handler.getResult();
    }

    private <T> String toXml(T item) throws IOException {
        AdolpheXmlWriter<T> writer = findXmlWriter((Class<T>) item.getClass());
        StringWriter sw = new StringWriter();
        writer.write(item, sw);
        return sw.toString();
    }

    private <T> void toXml(T item, OutputStream os) throws IOException {
        AdolpheXmlWriter<T> writer = findXmlWriter((Class<T>) item.getClass());
        OutputStreamWriter osw = new OutputStreamWriter(os);
        writer.write(item, osw);
    }

    private <T> void toXml(T item, File file) throws IOException {
        AdolpheXmlWriter<T> writer = findXmlWriter((Class<T>) item.getClass());
        FileWriter fw = new FileWriter(file);
        writer.write(item, fw);
    }

    private <T> void toXml(T item, Writer sw) throws IOException {
        AdolpheXmlWriter<T> writer = findXmlWriter((Class<T>) item.getClass());
        writer.write(item, sw);
    }

    private SAXParser newParser() throws SAXException {
        if (factory == null) factory = SAXParserFactory.newInstance();
        try {
            return factory.newSAXParser();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> FasaxHandler<T> findSaxHandler(Class<T> clazz) {
        FasaxHandler<T> saxHandler = SAX_HANDLERS.get(clazz);
        if (saxHandler == null) {
            String className = clazz.getName();
            try {
                Class<T> saxHandlerClass = (Class<T>) Class.forName(className + FasaxReaderGenerator.SUFFIX);
                saxHandler = (FasaxHandler<T>) saxHandlerClass.newInstance();
                SAX_HANDLERS.put(clazz, saxHandler);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return saxHandler;
    }

    private <T> AdolpheXmlWriter<T> findXmlWriter(Class<T> clazz) {
        AdolpheXmlWriter<T> xmlWriter = XML_WRITERS.get(clazz);
        if (xmlWriter == null) {
            String className = clazz.getName();
            try {
                Class<T> xmlClass = (Class<T>) Class.forName(className + FasaxWriterGenerator.SUFFIX);
                xmlWriter = (AdolpheXmlWriter<T>) xmlClass.newInstance();
                XML_WRITERS.put(clazz, xmlWriter);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return xmlWriter;
    }
}
