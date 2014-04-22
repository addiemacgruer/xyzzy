
package uk.addie.xyzzy.util;

import android.text.TextPaint;
import android.util.Log;

public class FontWidth {
    public static int widthOfString(String s, int textSize) {
        TextPaint tp = new TextPaint();
        tp.setTextSize(textSize);
        int rval = (int) tp.measureText(s);
        Log.d("Xyzzy", "Width of " + s + "=" + rval);
        return rval;
    }
}
