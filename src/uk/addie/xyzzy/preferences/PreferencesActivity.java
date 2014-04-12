
package uk.addie.xyzzy.preferences;

import uk.addie.xyzzy.R;
import android.app.Activity;
import android.content.SharedPreferences;
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

    @Override public void onClick(View view) {
        Log.i("Xyzzy", "Clicked:" + view);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences);
        enableSound = (CheckBox) findViewById(R.id.enableSound);
        enableSound.setOnClickListener(this);
        seekBar = (SeekBar) findViewById(R.id.fontsizeSeekbar);
        seekBar.setOnTouchListener(this);
        seekBar.setMax(maxTextSize - minTextSize);
        SharedPreferences xyzzyPrefs = getSharedPreferences("Xyzzy", 0);
        int textSize = xyzzyPrefs.getInt("textSize", 16);
        seekBar.setProgress(textSize - minTextSize);
        updateTextSize(textSize);
    }

    @Override public boolean onTouch(View v, MotionEvent event) {
        if (v == seekBar) {
            int newTextSize = seekBar.getProgress() + minTextSize;
            updateTextSize(newTextSize);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                saveTextSize(newTextSize);
            }
        } else {
            Log.i("Xyzzy", "Touched:" + v + " :" + event);
        }
        return false;
    }

    private void saveTextSize(int newTextSize) {
        Log.w("Xyzzy", "New text size:" + newTextSize);
        SharedPreferences.Editor sp = getSharedPreferences("Xyzzy", 0).edit();
        sp.putInt("textSize", newTextSize);
        sp.commit();
    }

    void updateTextSize(int textSize) {
        TextView textSizeView = (TextView) findViewById(R.id.fontsizeTextbox);
        textSizeView.setText("Font size: " + textSize);
    }
}
