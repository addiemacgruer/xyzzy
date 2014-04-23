
package uk.addie.xyzzy.util;

import uk.addie.xyzzy.preferences.Preferences;
import android.app.Activity;
import android.graphics.Typeface;
import android.text.TextPaint;

public class FontWidth {
    public static double widthOfMonospacedString(Activity a, String s) {
        final float densityMultiplier = a.getResources().getDisplayMetrics().scaledDensity;
        final int textSize = (Integer) Preferences.TEXT_SIZE.getValue(a);
        TextPaint tp = new TextPaint();
        tp.setTypeface(Typeface.MONOSPACE);
        tp.setTextSize(textSize);
        double rval = (tp.measureText(s) * densityMultiplier);
        //        Log.d("Xyzzy", "Width of " + s + "=" + rval + " @" + densityMultiplier);
        return rval;
    }

    public static double widthOfString(Activity a, String s) {
        final float densityMultiplier = a.getResources().getDisplayMetrics().scaledDensity;
        final int textSize = (Integer) Preferences.TEXT_SIZE.getValue(a);
        TextPaint tp = new TextPaint();
        tp.setTextSize(textSize);
        double rval = (tp.measureText(s) * densityMultiplier);
        //        Log.d("Xyzzy", "Width of " + s + "=" + rval + " @" + densityMultiplier);
        return rval;
    }
}
