package me.tatarka.fasaxandroid;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import me.tatarka.fasaxandroid.model.Author;
import me.tatarka.fasaxandroid.model.Content;
import me.tatarka.fasaxandroid.model.Tweet;
import me.tatarka.fasaxandroid.model.Tweets;

public class XPathTweetsParser {

    private DocumentBuilder builder;
    private XPathFactory factory;

    private XPathExpression entry;
    private XPathExpression published;
    private XPathExpression title;
    private XPathExpression contentType;
    private XPathExpression content;
    private XPathExpression lang;
    private XPathExpression authorName;
    private XPathExpression authorUri;

    private DateFormat dateFormat;

    public XPathTweetsParser()
            throws Exception {
        DocumentBuilderFactory _dbf = DocumentBuilderFactory.newInstance();
        _dbf.setNamespaceAware(true);
        builder = _dbf.newDocumentBuilder();
        factory = XPathFactory.newInstance();

        NamespaceContext _ctx = new NamespaceContext() {
            public String getNamespaceURI(String aPrefix) {
                String _uri;
                if (aPrefix.equals("atom"))
                    _uri = "http://www.w3.org/2005/Atom";
                else if (aPrefix.equals("twitter"))
                    _uri = "http://api.twitter.com/";
                else
                    _uri = null;
                return _uri;
            }

            @Override
            public String getPrefix(String aArg0) {
                return null;
            }

            @Override
            @SuppressWarnings("rawtypes")
            public Iterator getPrefixes(String aArg0) {
                return null;
            }
        };

        entry = newXPath(factory, _ctx, "/atom:feed/atom:entry");
        published = newXPath(factory, _ctx, ".//atom:published");
        title = newXPath(factory, _ctx, ".//atom:title");
        contentType = newXPath(factory, _ctx, ".//atom:content/@type");
        content = newXPath(factory, _ctx, ".//atom:content");
        lang = newXPath(factory, _ctx, ".//twitter:lang");
        authorName = newXPath(factory, _ctx, ".//atom:author/atom:name");
        authorUri = newXPath(factory, _ctx, ".//atom:author/atom:uri");

        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    }

    private XPathExpression newXPath(XPathFactory aFactory, NamespaceContext aCtx, String anXPath)
            throws Exception {
        XPath _xp = factory.newXPath();
        _xp.setNamespaceContext(aCtx);
        return _xp.compile(anXPath);
    }

    public Tweets read(InputStream anInputStream)
            throws Exception {
        Tweets _result = new Tweets();
        Document _document = builder.parse(anInputStream);

        NodeList _entries = (NodeList) entry.evaluate(_document, XPathConstants.NODESET);
        for (int i = 0; i < _entries.getLength(); i++) {
            Tweet _tweet = new Tweet();
            _result.addTweet(_tweet);

            Node _entryNode = _entries.item(i);

            _tweet.setPublished(getPublishedDate(_entryNode));
            _tweet.setTitle(title.evaluate(_entryNode));
            _tweet.setLanguage(lang.evaluate(_entryNode));

            Content _c = new Content();
            _tweet.setContent(_c);

            _c.setType(contentType.evaluate(_entryNode));
            _c.setValue(content.evaluate(_entryNode));

            Author _a = new Author();
            _tweet.setAuthor(_a);

            _a.setName(authorName.evaluate(_entryNode));
            _a.setUri(authorUri.evaluate(_entryNode));
        }

        return _result;
    }

    private Date getPublishedDate(Node aNode)
            throws Exception {
        return dateFormat.parse(published.evaluate(aNode));
    }
}
