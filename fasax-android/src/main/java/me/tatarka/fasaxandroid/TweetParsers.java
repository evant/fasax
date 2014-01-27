package me.tatarka.fasaxandroid;

import android.util.Log;

import com.sjl.dsl4xml.SAXDocumentReader;
import com.sjl.dsl4xml.support.convert.ThreadSafeDateConverter;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.util.Date;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import me.tatarka.fasax.Fasax;
import me.tatarka.fasaxandroid.model.Author;
import me.tatarka.fasaxandroid.model.Content;
import me.tatarka.fasaxandroid.model.DateConverter;
import me.tatarka.fasaxandroid.model.Tweet;
import me.tatarka.fasaxandroid.model.Tweets;

import static com.sjl.dsl4xml.SAXDocumentReader.attributes;
import static com.sjl.dsl4xml.SAXDocumentReader.mappingOf;
import static com.sjl.dsl4xml.SAXDocumentReader.tag;

public final class TweetParsers {
    private TweetParsers() {
    }

    public static Parser<Tweets> simpleXml() {
        // simple xml setup
        Registry registry = new Registry();
        Strategy strategy = new RegistryStrategy(registry);
        try {
            registry.bind(Date.class, new DateConverter());
        } catch (Exception e) {
            Log.e("Fasax", e.getMessage(), e);
            return null;
        }

        final Serializer simple = new Persister(strategy);

        return new Parser<Tweets>() {
            @Override
            public Tweets parse(InputStream in) throws Exception {
                return simple.read(Tweets.class, in);
            }
        };
    }

    public static Parser<Tweets> dom() {
        final DOMTweetsParser parser;
        try {
            parser = new DOMTweetsParser();
        } catch (Exception e) {
            Log.e("Fasax", e.getMessage(), e);
            return null;
        }

        return new Parser<Tweets>() {
            @Override
            public Tweets parse(InputStream in) throws Exception {
                return parser.read(in);
            }
        };
    }

    public static Parser<Tweets> xpath() {
        final XPathTweetsParser parser;
        try {
            parser = new XPathTweetsParser();
        } catch (Exception e) {
            Log.e("Fasax", e.getMessage(), e);
            return null;
        }

        return new Parser<Tweets>() {
            @Override
            public Tweets parse(InputStream in) throws Exception {
                return parser.read(in);
            }
        };
    }

    public static Parser<Tweets> dsl4xml() {
        final SAXDocumentReader<Tweets> dsl4xml = mappingOf("feed", Tweets.class).to(
                tag("entry", Tweet.class).with(
                        tag("published"),
                        tag("title"),
                        tag("content", Content.class).with(
                                attributes("type")
                        ).withPCDataMappedTo("value"),
                        tag("twitter:lang").
                                withPCDataMappedTo("language"),
                        tag("author", Author.class).with(
                                tag("name"),
                                tag("uri")
                        )
                )
        );
        dsl4xml.registerConverters(new ThreadSafeDateConverter("yyyy-MM-dd'T'HH:mm:ss"));

        return new Parser<Tweets>() {
            @Override
            public Tweets parse(InputStream in) throws Exception {
                return dsl4xml.read(in, "utf-8");
            }
        };
    }

    public static Parser<Tweets> fasax() {
        final Fasax fasax = new Fasax();

        return new Parser<Tweets>() {
            @Override
            public Tweets parse(InputStream in) throws Exception {
                return fasax.fromXml(in, Tweets.class);
            }
        };
    }

    public static Parser<Tweets> sax() {
        try {
            final XMLReader saxReader;
            final TweetsHandler handler = new TweetsHandler();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxReader = saxParser.getXMLReader();
            saxReader.setContentHandler(handler);

            return new Parser<Tweets>() {
                @Override
                public Tweets parse(InputStream in) throws Exception {
                    saxReader.parse(new InputSource(in));
                    return handler.getResult();
                }
            };
        } catch (Exception e) {
            Log.e("Error", e.getMessage(), e);
            return null;
        }
    }
}
