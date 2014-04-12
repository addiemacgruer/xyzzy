
package uk.addie.xyzzy.preferences;

import uk.addie.xyzzy.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

public class PreferencesActivity extends Activity implements OnClickListener, OnTouchListener {
    private SeekBar          seekBar;
    private CheckBox         enableSound;
    private static final int minTextSize = 6;
    private static final int maxTextSize = 48;
    private CheckBox         reportMinor;
    private CheckBox         useColour;

    private CheckBox initialiseCheckbox(int viewId, Preferences pref) {
        CheckBox rval = (CheckBox) findViewById(viewId);
        rval.setOnClickListener(this);
        rval.setChecked((Boolean) pref.getValue(this));
        return rval;
    }

    private void initialiseFontSizeBar() {
        seekBar = (SeekBar) findViewById(R.id.fontsizeSeekbar);
        seekBar.setOnTouchListener(this);
        seekBar.setMax(maxTextSize - minTextSize);
        int textSize = (Integer) Preferences.TEXT_SIZE.getValue(this);
        seekBar.setProgress(textSize - minTextSize);
        updateTextSize(textSize);
    }

    @Override public void onClick(View view) {
        if (view == enableSound) {
            Preferences.SOUND_ON.setValue(this, enableSound.isChecked());
        } else if (view == reportMinor) {
            Preferences.REPORT_MINOR.setValue(this, reportMinor.isChecked());
        } else if (view == useColour) {
            Preferences.USE_COLOUR.setValue(this, useColour.isChecked());
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences);
        enableSound = initialiseCheckbox(R.id.enableSound, Preferences.SOUND_ON);
        reportMinor = initialiseCheckbox(R.id.reportMinor, Preferences.REPORT_MINOR);
        useColour = initialiseCheckbox(R.id.useColour, Preferences.USE_COLOUR);
        initialiseFontSizeBar();
    }

    @Override public boolean onTouch(View v, MotionEvent event) {
        if (v == seekBar) {
            int newTextSize = seekBar.getProgress() + minTextSize;
            updateTextSize(newTextSize);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Preferences.TEXT_SIZE.setValue(this, newTextSize);
            }
        } else {
            Log.i("Xyzzy", "Touched:" + v + " :" + event);
        }
        return false;
    }

    void updateTextSize(int textSize) {
        TextView textSizeView = (TextView) findViewById(R.id.fontsizeTextbox);
        textSizeView.setText("Font size: " + textSize);
    }
}
