
package uk.addie.xyzzy.zmachine;

import java.io.Serializable;

import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.zobjects.TextStyle;
import android.util.Log;

public class ZStream implements Serializable {
    private static final long          serialVersionUID = 1L;
    private final ZStack<StringBuffer> stream3output    = new ZStack<StringBuffer>(null);
    private final ZStack<Integer>      stream3position  = new ZStack<Integer>(0);
    private final boolean[]            streams          = { false, true, false, false, false };

    public void addStyle(final TextStyle textStyle) {
        if (!streams[3] && streams[1]) {
            Memory.current().zwin.get(Memory.current().currentScreen).addStyle(textStyle);
        }
    }

    public void append(final String string) {
        if (!streams[3] && streams[1]) {
            Memory.current().zwin.get(Memory.current().currentScreen).append(string);
        }
        if (streams[3]) {
            stream3output.peek().append(string);
        }
    }

    public void clearStyles() {
        if (!streams[3] && streams[1]) {
            Memory.current().zwin.get(Memory.current().currentScreen).clearStyles();
        }
    }

    public void eraseLine(final int line) {
        if (!streams[3] && streams[1]) {
            Memory.current().zwin.get(Memory.current().currentScreen).eraseLine(line);
        }
    }

    public void println() {
        if (!streams[3] && streams[1]) {
            Memory.current().zwin.get(Memory.current().currentScreen).println();
        }
        if (streams[3]) {
            stream3output.peek().append((char) 13);
        }
    }

    @SuppressWarnings("static-method") public String promptForInput() {
        return Memory.current().zwin.get(Memory.current().currentScreen).promptForInput();
    }

    public void setBuffered(final boolean buffered) {
        if (!streams[3] && streams[1]) {
            Memory.current().zwin.get(Memory.current().currentScreen).setBuffered(buffered);
        }
    }

    public void setCursor(final short column, final short line) {
        if (!streams[3] && streams[1]) {
            Memory.current().zwin.get(Memory.current().currentScreen).setCursor(column, line);
        }
    }

    /**
     * @param width
     *            is justification in v6
     */
    public void setOutputStream(final int number, final int table, final int width) {
        if (number == 0) {
            return;
        }
        streams[Math.abs(number)] = number > 0;
        if (number == 3) { // enable stream 3
            stream3position.add(table);
            stream3output.add(new StringBuffer());
            if (stream3position.size() >= 17) { //overflow
                Error.STR3_NESTING.invoke();
            }
        } else if (number == -3) {//disable stream 3
            final int position = stream3position.pop();
            final StringBuffer output = stream3output.pop();
            Memory.current().buffer.putShort(position, output.length());
            for (int i = 0, j = output.length(); i < j; i++) {
                Memory.current().buffer.put(position + 2 + i, output.charAt(i));
            }
        }
    }

    public void userInput(final String string) {
        Log.i("Xyzzy", "User input:" + string);
        if (streams[4]) {
            throw new UnsupportedOperationException("Writing user transcript");
        }
    }
}
