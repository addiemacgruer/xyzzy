
package uk.addie.xyzzy.preferences;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class BooleanValueView extends CheckBox implements OnClickListener {
    private final Preferences preferences;

    BooleanValueView(Context context, Preferences p) {
        super(context);
        this.preferences = p;
        setText(p.prefDescription());
        setTextSize(PreferencesActivity.activity.textSize);
        setChecked((Boolean) p.getValue(PreferencesActivity.activity));
        setPadding(0, PreferencesActivity.activity.textSize * 2, 0, PreferencesActivity.activity.textSize * 2);
        setOnClickListener(this);
    }

    @Override public void onClick(View v) {
        preferences.setValue(PreferencesActivity.activity, isChecked());
    }
}
