package me.tatarka.fasaxandroid;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import me.tatarka.fasaxandroid.model.Author;
import me.tatarka.fasaxandroid.model.Content;
import me.tatarka.fasaxandroid.model.Tweet;
import me.tatarka.fasaxandroid.model.Tweets;

public class TweetsHandler extends DefaultHandler {
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private Tweets tweets;
    private Tweet tweet;
    private Content content;
    private Author author;
    private String currentElement;
    private StringBuilder chars;

    public Tweets getResult() {
        return tweets;
    }

    @Override
    public void startDocument() throws SAXException {
        chars = new StringBuilder();
        tweets = new Tweets();
    }

    @Override
    public void startElement(
            String aUri, String aLocalName,
            String aQName, Attributes aAttributes
    ) throws SAXException {
        currentElement = aQName;
        chars.setLength(0);

        if ("entry".equals(aQName)) {
            tweets.addTweet(tweet = new Tweet());
        } else if ("content".equals(aQName)) {
            tweet.setContent(content = new Content());
            content.setType(aAttributes.getValue("type"));
        } else if ("author".equals(aQName)) {
            tweet.setAuthor(author = new Author());
        }
    }

    @Override
    public void endElement(String aUri, String aLocalName, String aQName)
            throws SAXException {
        if (chars.length() > 0) {
            setCharacterValue(chars);
        }

        currentElement = null;
    }

    @Override
    public void characters(char[] aCh, int aStart, int aLength)
            throws SAXException {
        chars.append(aCh, aStart, aLength);
    }

    private void setCharacterValue(StringBuilder aCharacters)
            throws SAXException {
        if ("published".equals(currentElement)) {
            try {
                tweet.setPublished(dateFormat.parse(aCharacters.toString()));
            } catch (ParseException anExc) {
                throw new SAXException(anExc);
            }
        } else if (("title".equals(currentElement)) && (tweet != null)) {
            tweet.setTitle(aCharacters.toString());
        } else if ("content".equals(currentElement)) {
            content.setValue(aCharacters.toString());
        } else if ("twitter:lang".equals(currentElement)) {
            tweet.setLanguage(aCharacters.toString());
        } else if ("name".equals(currentElement)) {
            author.setName(aCharacters.toString());
        } else if ("uri".equals(currentElement)) {
            author.setUri(aCharacters.toString());
        }
    }
}

