package uk.addie.xyzzy.preferences;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ScrollValueView extends LinearLayout implements OnTouchListener {
  ScrollValueView(final Context context, final Preferences p) {
    super(context);
    setOrientation(VERTICAL);
    textView = new TextView(context);
    addView(textView);
    seekBar = new SeekBar(context);
    final int currentValue = (Integer) p.getValue(PreferencesActivity.activity);
    seekBar.setMax(p.max() - p.min());
    seekBar.setProgress(currentValue - p.min());
    seekBar.setOnTouchListener(this);
    addView(seekBar);
    textView.setTextSize(PreferencesActivity.activity.textSize);
    textView.setText(p.prefDescription() + ": " + currentValue);
    setPadding(0, PreferencesActivity.activity.textSize * 2, 0,
        PreferencesActivity.activity.textSize * 2);
    preferences = p;
  }

  private final Preferences preferences;

  private final TextView textView;

  private final SeekBar seekBar;

  @Override public boolean onTouch(final View v, final MotionEvent event) {
    final int newTextSize = seekBar.getProgress() + preferences.min();
    textView.setText(preferences.prefDescription() + ": " + newTextSize);
    if (event.getAction() == MotionEvent.ACTION_UP) {
      preferences.setValue(PreferencesActivity.activity, newTextSize);
    }
    return false;
  }
}
