package me.tatarka.fasaxandroid;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import me.tatrka.fasaxandroid.R;

public class TestResultViewModel extends FrameLayout {
    public static final int LAYOUT = R.layout.test_result;

    private TextView mName;
    private TextView mTime;
    private ProgressBar mProgress;

    public TestResultViewModel(Context context) {
        super(context);
    }

    public TestResultViewModel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TestResultViewModel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mName = (TextView) findViewById(R.id.name);
        mTime = (TextView) findViewById(R.id.time);
        mProgress = (ProgressBar) findViewById(R.id.progress);
    }

    public void populate(TestParser result) {
        mName.setText(result.name);
        mTime.setText(String.format("%.2f", (result.time / 10000000L) / 100f));
        mProgress.setProgress(result.percent);
    }
}
