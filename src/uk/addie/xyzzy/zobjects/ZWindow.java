
package uk.addie.xyzzy.zobjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import uk.addie.xyzzy.MainActivity;
import uk.addie.xyzzy.R;
import uk.addie.xyzzy.os.Debug;
import uk.addie.xyzzy.state.Memory;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class ZWindow implements Serializable {
    enum DisplayState {
        EMPTY, FLUSH_UNSETCURSOR, FLUSH_SETCURSOR, DRAWN_ON;
    }

    private static int                      background;
    private final static SparseIntArray     colours          = new SparseIntArray();
    private static int                      foreground;
    private final static View.OnKeyListener okl;
    private static final long               serialVersionUID = 1L;
    public final static int                 textSize         = 16;
    private static int[]                    windowMap        = { R.id.screen0, R.id.screen1 };
    private static long                     latency          = 0;
    static {
        final int[] amigaColours = { 0x0, 0x7fff, 0x0, 0x1d, 0x340, 0x3bd, 0x59a0, 0x7c1f, 0x77a0, 0x7fff, 0x5ad6,
            0x4631, 0x2d6b };
        colours.put(-1, 0x0); // transparent
        for (int i = 0; i < amigaColours.length; i++) {
            colours.put(i, amigaColourToAndroid(amigaColours[i]));
        }
        okl = new View.OnKeyListener() {
            @Override public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                    synchronized (MainActivity.inputSyncObject) {
                        MainActivity.inputSyncObject.notifyAll();
                    }
                    return true;
                }
                return false;
            }
        };
    }
    private final static double             amiAndroidRatio  = 255.0 / 31.0;                  // ratio 0xff to 0x1f

    private static int amigaColourToAndroid(int amiga) {
        int red = (int) ((0x1f & amiga) * amiAndroidRatio); // bottom five bits, scaled from 0x1f to 0xff
        int green = (int) (((0x3e0 & amiga) >> 5) * amiAndroidRatio);
        int blue = (int) (((0x7c00 & amiga) >> 10) * amiAndroidRatio);
        int androidValue = 0xff000000 | (red << 16) | (green << 8) | blue;
        return androidValue;
    }

    public static void defaultColours() {
        foreground = colours.get(0);
        background = colours.get(1);
    }

    private static EditText formattedEditText() {
        final EditText et = new EditText(MainActivity.activity.getApplicationContext());
        et.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        et.setTextColor(background);
        et.setTextSize(textSize);
        et.setPadding(0, 0, 0, 0);
        et.setBackgroundColor(foreground);
        et.setImeActionLabel(">", 0);
        et.setHorizontallyScrolling(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setOnKeyListener(okl);
        return et;
    }

    public static void printAllScreens() {
        int zwc = Memory.current().zwin.size();
        for (int i = 0; i < zwc; i++) {
            Memory.current().zwin.get(Memory.current().zwin.keyAt(i)).flush();
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

    private transient List<SpannableStringBuilder> buffer           = new ArrayList<SpannableStringBuilder>();
    private boolean                                buffered         = true;
    private final Map<TextStyle, Integer>          currentTextStyle = new EnumMap<TextStyle, Integer>(TextStyle.class);
    private final int                              windowCount;
    private int                                    row, column;
    DisplayState                                   displayState     = DisplayState.EMPTY;

    public ZWindow(final int window) {
        windowCount = window;
    }

    public void addStyle(final TextStyle ts) {
        currentTextStyle.put(ts, column);
    }

    public void append(final String s) {
        if (displayState == DisplayState.FLUSH_UNSETCURSOR) {
            clearBuffer();
        }
        displayState = DisplayState.DRAWN_ON;
        while (buffer.size() <= row) {
            buffer.add(new SpannableStringBuilder());
        }
        if (buffer.get(row).length() > column) { // aargh!  but status lines (and Anchorhead) require this.
            SpannableStringBuilder oldString = buffer.get(row);
            buffer.set(row, new SpannableStringBuilder(oldString.subSequence(0, column)));
            buffer.get(row).append(s);
            column += s.length();
            if (oldString.length() > column) {
                buffer.get(row).append(oldString.subSequence(column, oldString.length()));
            }
            return;
        }
        while (buffer.get(row).length() < column) {
            buffer.get(row).append(' ');
        }
        buffer.get(row).append(s);
        column += s.length();
    }

    private void clearBuffer() {
        buffer.clear();
        row = 0;
        column = 0;
    }

    public void clearStyles() {
        if (row >= buffer.size()) {
            currentTextStyle.clear();
            return;
        }
        for (final TextStyle ts : currentTextStyle.keySet()) {
            final Integer start = currentTextStyle.get(ts);
            final int end = buffer.get(row).length();
            if (Debug.screen) {
                Log.i("Xyzzy", "Styling " + ts + " from " + start + " to " + end);
            }
            if (ts != TextStyle.REVERSE_VIDEO) {
                buffer.get(row).setSpan(ts.characterStyle(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                buffer.get(row).setSpan(new BackgroundColorSpan(foreground), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                buffer.get(row).setSpan(new ForegroundColorSpan(background), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        currentTextStyle.clear();
    }

    public void flush() {
        clearStyles();
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int bufferlength = buffer.size();
        for (int i = 0; i < bufferlength; i++) {
            ssb.append(buffer.get(i));
            ssb.append('\n');
        }
        if (ssb.length() > 0) {
            final TextView tv = textView();
            tv.setText(ssb);
            MainActivity.activity.addView(tv, windowMap[windowCount]);
            //            buffer.clear();
        }
        displayState = DisplayState.FLUSH_UNSETCURSOR;
        //        row = 0;
        //        column = 0;
    }

    public void println() {
        clearStyles();
        row++;
        buffer.add(new SpannableStringBuilder());
        column = 0;
    }

    public synchronized String promptForInput() {
        final EditText et = formattedEditText();
        MainActivity.activity.addView(et, windowMap[windowCount]);
        synchronized (MainActivity.inputSyncObject) {
            try {
                Log.i("Xyzzy", "Waiting for input..."
                        + (latency != 0 ? "(" + (System.currentTimeMillis() - latency) + " ms since last)" : ""));
                MainActivity.inputSyncObject.wait();
            } catch (final InterruptedException e) {
                // don't care if interrupted.
            }
        }
        final String command = et.getText().toString() + "\n";
        latency = System.currentTimeMillis();
        MainActivity.activity.finishEditing(et, foreground, background);
        return command;
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        foreground = in.readInt();
        background = in.readInt();
        buffer = new ArrayList<SpannableStringBuilder>();
    }

    public void reset() {
        MainActivity.activity.removeChildren(windowMap[windowCount]);
    }

    public void setBuffered(final boolean buffered) {
        Log.i("Xyzzy", "Set buffered:" + this + "=" + buffered);
        this.buffered = buffered;
    }

    public void setCursor(short column, short line) {
        clearStyles();
        if (displayState == DisplayState.FLUSH_UNSETCURSOR) {
            displayState = DisplayState.FLUSH_SETCURSOR;
        }
        if (column < this.column) {
            reset();
        }
        this.column = column;
        this.row = line;
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
