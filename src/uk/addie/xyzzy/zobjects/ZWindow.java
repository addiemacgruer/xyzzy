
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
import uk.addie.xyzzy.preferences.Preferences;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.zmachine.Decoder;
import android.graphics.Point;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

public class ZWindow implements Serializable {
    private enum DisplayState {
        DRAWN_ON, EMPTY, FLUSH_SETCURSOR, FLUSH_UNSETCURSOR;
    }

    private final static double         amiAndroidRatio  = 255.0 / 31.0;              // ratio 0xff to 0x1f
    public static int                   background;
    private final static SparseIntArray colours          = new SparseIntArray();
    public static int                   foreground;
    private static long                 latency          = System.currentTimeMillis();
    private static final long           serialVersionUID = 1L;
    private static int[]                windowMap        = { R.id.screen0, R.id.screen1, R.id.screen2, R.id.screen3,
            R.id.screen4, R.id.screen5, R.id.screen6, R.id.screen7 };
    static {
        final int[] amigaColours = { 0x0, 0x7fff, 0x0, 0x1d, 0x340, 0x3bd, 0x59a0, 0x7c1f, 0x77a0, 0x7fff, 0x5ad6,
            0x4631, 0x2d6b };
        colours.put(-1, 0x0); // transparent
        for (int i = 0; i < amigaColours.length; i++) {
            colours.put(i, amigaColourToAndroid(amigaColours[i]));
        }
    }

    private static int amigaColourToAndroid(final int amiga) {
        final int red = (int) ((0x1f & amiga) * amiAndroidRatio); // bottom five bits, scaled from 0x1f to 0xff
        final int green = (int) (((0x3e0 & amiga) >> 5) * amiAndroidRatio);
        final int blue = (int) (((0x7c00 & amiga) >> 10) * amiAndroidRatio);
        final int androidValue = 0xff000000 | red << 16 | green << 8 | blue;
        return androidValue;
    }

    public static void defaultColours() {
        foreground = colours.get(0);
        background = colours.get(1);
    }

    public static void keyWaitLag(long milliseconds) {
        latency += milliseconds;
    }

    public static void printAllScreens() {
        final SparseArray<ZWindow> zwindows = Memory.current().zwin;
        final int zwc = zwindows.size();
        for (int i = 0; i < zwc; i++) {
            zwindows.get(zwindows.keyAt(i)).flush();
        }
    }

    public static void setColour(final int fore, final int back) {
        foreground = colours.get(fore);
        background = colours.get(back);
        MainActivity.activity.setBackgroundColour(background);
    }

    public static void setTrueColour(final int foreground2, final int background2) {
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
        MainActivity.activity.setBackgroundColour(background);
    }

    private transient List<SpannableStringBuilder> buffer           = new ArrayList<SpannableStringBuilder>();
    private final Map<TextStyle, Integer>          currentTextStyle = new EnumMap<TextStyle, Integer>(TextStyle.class);
    private DisplayState                           displayState     = DisplayState.EMPTY;
    private int                                    naturalHeight    = 1;
    private int                                    row, column;
    private final int                              windowCount;

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
            final SpannableStringBuilder oldString = buffer.get(row);
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
            if (end <= start) {
                Log.e("Xyzzy", "Trying to set bad style spans");
                continue;
            }
            if (ts != TextStyle.REVERSE_VIDEO) {
                buffer.get(row).setSpan(ts.characterStyle(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (start < end) {
                buffer.get(row).setSpan(new BackgroundColorSpan(foreground), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                buffer.get(row).setSpan(new ForegroundColorSpan(background), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        currentTextStyle.clear();
    }

    public Point cursorPosition() {
        return new Point(column, row);
    }

    public void eraseLine(final int line) {
        if (line == 1) { // erase line from current cursor to end of row.
            if (row >= buffer.size()) {
                return;
            }
            final SpannableStringBuilder currentRow = buffer.get(row);
            final int currentRowLength = currentRow.length();
            if (currentRowLength <= column) {
                return;
            }
            final SpannableStringBuilder replacementRow = new SpannableStringBuilder(currentRow.subSequence(0, column));
            buffer.set(row, replacementRow);
        } // otherwise, do nothing.
    }

    public void flush() {
        clearStyles();
        final SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (int i = 0, bufferlength = buffer.size(); i < bufferlength; i++) {
            if (i != 0) {
                ssb.append('\n');
            }
            ssb.append(buffer.get(i));
        }
        if (windowCount > 0) {
            reset();
            this.column = this.row = 0;
            displayState = DisplayState.FLUSH_SETCURSOR;
            while (buffer.size() > naturalHeight) {
                buffer.remove(naturalHeight);
            }
        } else {
            displayState = DisplayState.FLUSH_UNSETCURSOR;
        }
        if (ssb.length() > 0) {
            MainActivity.activity.addTextView(ssb, foreground, background, windowMap[windowCount]);
        }
    }

    public void println() {
        List<TextStyle> stylesInEffect = storeStylesAndClear();
        row++;
        column = 0;
        restoreStyles(stylesInEffect);
    }

    private void printPerformanceInformation() {
        StringBuilder sb = new StringBuilder();
        sb.append("*** Opcode count:");
        sb.append(Decoder.opcount());
        if (latency != 0) {
            int calctime = (int) (System.currentTimeMillis() - latency);
            sb.append(" calc time:");
            sb.append(calctime);
            sb.append(" ms = ");
            sb.append(Decoder.opcount() * 1000 / calctime);
            sb.append(" ops/s");
        }
        MainActivity.activity.addTextView(new SpannableStringBuilder(sb.toString()), foreground, background,
                windowMap[windowCount]);
        Decoder.resetOpcount();
    }

    public synchronized String promptForInput() {
        if ((Boolean) Preferences.MONITOR_PERFORMANCE.getValue(MainActivity.activity)) {
            printPerformanceInformation();
        }
        MainActivity.activity.addEditView(foreground, background, windowMap[windowCount]);
        String command;
        synchronized (MainActivity.inputSyncObject) {
            try {
                Log.i("Xyzzy", "Waiting for input..."
                        + (latency != 0 ? "(" + (System.currentTimeMillis() - latency) + " ms since last)" : ""));
                MainActivity.inputSyncObject.setString(null);
                MainActivity.inputSyncObject.wait();
            } catch (final InterruptedException e) {
                Log.e("Xyzzy", "Wait on string interrupted:", e);
            }
            command = MainActivity.inputSyncObject.string();
        }
        if (command == null) {
            command = "";
        }
        latency = System.currentTimeMillis();
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

    private void restoreStyles(List<TextStyle> stylesInEffect) {
        if (stylesInEffect == null) {
            return;
        }
        for (TextStyle ts : stylesInEffect) {
            addStyle(ts);
        }
    }

    /**
     * we buffer in all conditions to avoid large-size text from scrolling off the screen
     * 
     * @param buffered
     */
    public void setBuffered(final boolean buffered) {
        // NO-OP
    }

    public void setCursor(final int column, final int line) {
        if (displayState == DisplayState.FLUSH_UNSETCURSOR) {
            displayState = DisplayState.FLUSH_SETCURSOR;
        }
        List<TextStyle> stylesInEffect = storeStylesAndClear();
        // games will occasionally request negative indexes, especially if the screen is too narrow
        this.column = Math.max(column, 1) - 1;
        row = Math.max(line, 1) - 1;
        restoreStyles(stylesInEffect);
    }

    public void setNaturalHeight(final int lines) {
        naturalHeight = lines;
    }

    private List<TextStyle> storeStylesAndClear() {
        List<TextStyle> stylesInEffect = null;
        if (!currentTextStyle.isEmpty()) {
            stylesInEffect = new ArrayList<TextStyle>(currentTextStyle.keySet());
            clearStyles();
        }
        return stylesInEffect;
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
