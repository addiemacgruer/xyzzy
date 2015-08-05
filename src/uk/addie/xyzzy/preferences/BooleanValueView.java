package uk.addie.xyzzy.preferences;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

class BooleanValueView extends CheckBox implements OnClickListener {
  BooleanValueView(final Context context, final Preferences p) {
    super(context);
    preferences = p;
    setText(p.prefDescription());
    setTextSize(PreferencesActivity.activity.textSize);
    setChecked((Boolean) p.getValue(PreferencesActivity.activity));
    setPadding(0, PreferencesActivity.activity.textSize * 2, 0,
        PreferencesActivity.activity.textSize * 2);
    setOnClickListener(this);
  }

  private final Preferences preferences;

  @Override public void onClick(final View v) {
    preferences.setValue(PreferencesActivity.activity, isChecked());
  }
}
