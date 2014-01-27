package me.tatarka.fasaxandroid.model;

import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

import me.tatarka.fasax.ElementList;
import me.tatarka.fasax.Xml;

@Xml
@Root(name="feed", strict=false)
public class Tweets {
    @ElementList(entry="entry", inline=true)
    @org.simpleframework.xml.ElementList(type=Tweet.class, name="entry", inline=true)
	public List<Tweet> tweets;

    public Tweets() {
        tweets = new ArrayList<Tweet>();
    }

    public void addTweet(Tweet aTweet) {
        tweets.add(aTweet);
    }
}