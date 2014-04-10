
package uk.addie.xyzzy.zobjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.R;
import uk.addie.xyzzy.os.Debug;
import uk.addie.xyzzy.state.Memory;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class ZWindow implements Serializable {
    public enum TextStyle {
        BOLD {
            @Override CharacterStyle characterStyle() {
                return new StyleSpan(Typeface.BOLD);
            }
        },
        FIXED_PITCH {
            @Override CharacterStyle characterStyle() {
                return new TypefaceSpan("monospace");
            }
        },
        ITALIC {
            @Override CharacterStyle characterStyle() {
                return new StyleSpan(Typeface.ITALIC);
            }
        },
        REVERSE_VIDEO {
            @Override CharacterStyle characterStyle() {
                return new TypefaceSpan("monospace");
            }
        };
        abstract CharacterStyle characterStyle();
    }

    private static int                  background;
    private final static SparseIntArray colours          = new SparseIntArray();
    private static int                  foreground;
    final static View.OnKeyListener     okl;
    private static final long           serialVersionUID = 1L;
    public final static Object          syncObject       = new Object();
    public final static int             textSize         = 16;
    private static int[]                windowMap        = { R.id.screen0, R.id.screen1 };
    private static long                 latency          = 0;
    static {
        colours.put(-1, 0x0); // transparent
        colours.put(0, amigaColourToAndroid(0x0000)); // interpreter def foreground
        colours.put(1, amigaColourToAndroid(0x7fff)); // interpreter def background
        colours.put(2, amigaColourToAndroid(0x0000)); //black
        colours.put(3, amigaColourToAndroid(0x001d)); // red
        colours.put(4, amigaColourToAndroid(0x0340)); //green
        colours.put(5, amigaColourToAndroid(0x03bd)); // yellow
        colours.put(6, amigaColourToAndroid(0x59a0)); // blue
        colours.put(7, amigaColourToAndroid(0x7c1f)); // magenta
        colours.put(8, amigaColourToAndroid(0x77a0)); // cyan
        colours.put(9, amigaColourToAndroid(0x7fff)); //white
        colours.put(10, amigaColourToAndroid(0x5ad6)); // light grey
        colours.put(11, amigaColourToAndroid(0x4631)); // medium grey
        colours.put(12, amigaColourToAndroid(0x2d6b)); // dark grey
    }
    static {
        okl = new View.OnKeyListener() {
            @Override public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                Log.e("Xyzzy", "TextView:" + v + " keyCode:" + keyCode + " KeyEvent:" + event);
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                    synchronized (syncObject) {
                        syncObject.notify();
                    }
                    return true;
                }
                return false;
            }
        };
    }

    public static int amigaColourToAndroid(int amiga) {
        int red = (0x1f & amiga) * 8; // bottom five bits, scaled from 0x1f to 0x2f
        int green = ((0x3e0 & amiga) >> 5) * 8;
        int blue = ((0x7c00 & amiga) >> 10) * 8;
        int androidValue = 0xff000000 | (red << 16) | (green << 8) | blue;
        return androidValue;
    }

    public static void defaultColours() {
        foreground = colours.get(0);
        background = colours.get(1);
    }

    public static void printAllScreens() {
        int zwc = Memory.CURRENT.zwin.size();
        for (int i = 0; i < zwc; i++) { // : Memory.CURRENT.zwin.keySet()) {
            Memory.CURRENT.zwin.get(Memory.CURRENT.zwin.keyAt(i)).flush();
        }
    }

    public static void setColour(final int fore, final int back) {
        foreground = colours.get(fore);
        background = colours.get(back);
        MainActivity.activity.setBackgroundColour(background);
    }

    public static void setTrueColour(int foreground2, int background2) {
        Log.i("Xyzzy", "Set true colour:" + Integer.toHexString(foreground2) + " :" + Integer.toHexString(background2));
        if (foreground2 > 0) {
            foreground = amigaColourToAndroid(foreground2);
        } else if (foreground2 == -1) {
            foreground = colours.get(0);
        }
        if (background2 > 0) {
            background = amigaColourToAndroid(background2);
        } else if (background2 == -1) {
            background = colours.get(1);
        }
        Log.i("Xyzzy", "Set as:" + Integer.toHexString(foreground) + " :" + Integer.toHexString(background));
        MainActivity.activity.setBackgroundColour(background);
    }

    private static TextView textView() {
        final TextView ett = new TextView(MainActivity.activity.getApplicationContext());
        ett.setTextColor(foreground);
        ett.setBackgroundColor(background);
        ett.setTextSize(textSize);
        ett.setPadding(0, 0, 0, 0);
        return ett;
    }

    private transient SpannableStringBuilder buffer           = new SpannableStringBuilder();
    private boolean                          buffered         = true;
    private final Map<TextStyle, Integer>    currentTextStyle = new EnumMap<TextStyle, Integer>(TextStyle.class);
    final int                                windowCount;

    public ZWindow(final int window) {
        windowCount = window;
    }

    public void addStyle(final TextStyle ts) {
        currentTextStyle.put(ts, buffer.length());
    }

    public void append(final String s) {
        buffer.append(s);
    }

    public boolean buffered() {
        return buffered;
    }

    public void clearStyles() {
        for (final TextStyle ts : currentTextStyle.keySet()) {
            final Integer start = currentTextStyle.get(ts);
            final int end = buffer.length();
            if (Debug.screen) {
                Log.i("Xyzzy", "Styling " + ts + " from " + start + " to " + end);
            }
            if (ts != TextStyle.REVERSE_VIDEO) {
                buffer.setSpan(ts.characterStyle(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                buffer.setSpan(new BackgroundColorSpan(foreground), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                buffer.setSpan(new ForegroundColorSpan(background), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        currentTextStyle.clear();
    }

    public void flush() {
        clearStyles();
        if (buffer.length() > 0) {
            final TextView tv = textView();
            tv.setText(buffer);
            MainActivity.activity.addView(tv, windowMap[windowCount]);
            buffer.clear();
        }
    }

    public void println() {
        buffer.append("\n");
    }

    public synchronized String promptForInput() {
        final EditText et = new EditText(MainActivity.activity.getApplicationContext());
        et.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        et.setTextColor(background);
        et.setTextSize(textSize);
        et.setPadding(0, 0, 0, 0);
        et.setBackgroundColor(foreground);
        et.setImeActionLabel(">", 0);
        //        et.setImeOptions(EditorInfo.IME);
        et.setHorizontallyScrolling(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setOnKeyListener(okl);
        MainActivity.activity.addView(et, windowMap[windowCount]);
        synchronized (syncObject) {
            try {
                Log.i("Xyzzy", "Waiting for input..."
                        + (latency != 0 ? "(" + (System.currentTimeMillis() - latency) + " ms since last)" : ""));
                syncObject.wait();
            } catch (final InterruptedException e) {
                // don't care if interrupted.
            }
        }
        final String command = et.getText().toString() + "\n";
        latency = System.currentTimeMillis();
        Log.i("Xyzzy", "command:" + command);
        MainActivity.activity.finishEditing(et, foreground, background);
        return command;
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        buffer = new SpannableStringBuilder();
        foreground = in.readInt();
        background = in.readInt();
    }

    public void reset() {
        MainActivity.activity.removeChildren(windowMap[windowCount]);
    }

    public void setBuffered(final boolean buffered) {
        this.buffered = buffered;
    }

    @Override public String toString() {
        return "ZWindow:" + windowCount;
    }

    @SuppressWarnings("static-method") private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(foreground);
        out.writeInt(background);
    }
}
