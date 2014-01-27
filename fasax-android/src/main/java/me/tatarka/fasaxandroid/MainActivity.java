package me.tatarka.fasaxandroid;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import me.tatarka.fasaxandroid.model.Tweets;
import me.tatrka.fasaxandroid.R;

public class MainActivity extends Activity {
    private Button mStartButton;
    private ResultAdapter mAdapter;
    private List<TestParser<Tweets>> mTestParsers = Arrays.asList(
//            new TestParser<Tweets>("XPath", TweetParsers.xpath()),
            new TestParser<Tweets>("simple xml", TweetParsers.simpleXml()),
            new TestParser<Tweets>("DOM", TweetParsers.dom()),
            new TestParser<Tweets>("dsl4xml", TweetParsers.dsl4xml()),
            new TestParser<Tweets>("fasax", TweetParsers.fasax()),
            new TestParser<Tweets>("SAX", TweetParsers.sax())
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartButton = (Button) findViewById(R.id.button);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ParseAsyncTask().execute();
            }
        });

        ListView resultList = (ListView) findViewById(R.id.results);
        View footerView = LayoutInflater.from(this).inflate(R.layout.test_footer, null, false);
        resultList.addFooterView(footerView);
        resultList.setAdapter(mAdapter = new ResultAdapter());
    }

    private class ResultAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mTestParsers.size();
        }

        @Override
        public TestParser getItem(int i) {
            return mTestParsers.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(MainActivity.this).inflate(TestResultViewModel.LAYOUT, viewGroup, false);
            }
            ((TestResultViewModel) view).populate(getItem(i));
            return view;
        }
    }

    private class ParseAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            for (TestParser<Tweets> testParser : mTestParsers) {
                testParser.time = 0;

                try {
                    for (int i = 0; i < 50; i++) {
                        InputStream stream = getResources().openRawResource(R.raw.twitter_atom);
                        long start = System.nanoTime();
                        Tweets tweets = testParser.parser.parse(stream);
                        testParser.time += System.nanoTime() - start;
                    }
                } catch (Exception e) {
                    Log.e("Fasax", e.getMessage(), e);
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mStartButton.setEnabled(false);
            findViewById(R.id.test_running).setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            findViewById(R.id.test_running).setVisibility(View.INVISIBLE);
            mStartButton.setEnabled(true);
            Toast.makeText(MainActivity.this, "Tweets parsed!", Toast.LENGTH_SHORT).show();

            long max = Long.MIN_VALUE;
            long min = Long.MAX_VALUE;
            for (TestParser<Tweets> testParser : mTestParsers) {
                if (testParser.time < min) min = testParser.time;
                if (testParser.time > max) max = testParser.time;
            }

            for (TestParser<Tweets> testParser : mTestParsers) {
                testParser.percent = (int) (testParser.time * 100L / max);
            }

            mAdapter.notifyDataSetChanged();
        }
    }
}
