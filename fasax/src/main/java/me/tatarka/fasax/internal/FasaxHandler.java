package me.tatarka.fasax.internal;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Locale;

public abstract class FasaxHandler<T> extends DefaultHandler {
    protected static final int ROOT = 0;

    protected StringBuilder characters = new StringBuilder();
    protected T result;
    protected int state;
    protected boolean inTag;

    public T getResult() {
        return result;
    }

    @Override
    public void startDocument() throws SAXException {
        state = ROOT;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        inTag = true;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        inTag = false;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (inTag) {
            characters.append(ch, start, length);
        }
    }

    protected static String toString(String str) {
        if (str == null) return null;
        return str.trim();
    }

    protected static boolean toBoolean(String str) throws SAXException {
        str = str.trim().toLowerCase(Locale.US);
        if (str.equals("true")) {
            return true;
        } else if (str.equals("false")) {
            return false;
        }

        throw new SAXException("Unrecognized boolean: " + str);
    }

    protected static int toInt(String str) throws SAXException {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            throw new SAXException(e);
        }
    }

    protected static float toFloat(String str) throws SAXException {
        try {
            return Float.parseFloat(str.trim());
        } catch (NumberFormatException e) {
            throw new SAXException(e);
        }
    }

    protected static double toDouble(String str) throws SAXException {
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            throw new SAXException(e);
        }
    }

    protected static char toChar(String str) throws SAXException {
        try {
            return str.trim().charAt(0);
        } catch (IndexOutOfBoundsException e) {
            throw new SAXException();
        }
    }
}
