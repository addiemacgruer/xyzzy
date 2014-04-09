
package uk.addie.xyzzy.zmachine;

import java.io.Serializable;

import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.zobjects.ZWindow.TextStyle;
import android.util.Log;

public class ZStream implements Serializable {
    private static final long serialVersionUID = 1L;
    boolean[]                 streams          = { false, true, false, false, false };
    int                       stream3position  = 0;
    StringBuffer              stream3output    = new StringBuffer();

    public void addStyle(TextStyle textStyle) {
        if (!streams[3] && streams[1]) {
            Memory.CURRENT.zwin.get(Memory.CURRENT.currentScreen).addStyle(textStyle);
        }
    }

    public void append(String string) {
        if (!streams[3] && streams[1]) {
            Memory.CURRENT.zwin.get(Memory.CURRENT.currentScreen).append(string);
        }
        if (streams[3]) {
            stream3output.append(string);
        }
    }

    public void clearStyles() {
        if (!streams[3] && streams[1]) {
            Memory.CURRENT.zwin.get(Memory.CURRENT.currentScreen).clearStyles();
        }
    }

    public void println() {
        if (!streams[3] && streams[1]) {
            Memory.CURRENT.zwin.get(Memory.CURRENT.currentScreen).println();
        }
        if (streams[3]) {
            stream3output.append((char) 13);
        }
    }

    @SuppressWarnings("static-method") public String promptForInput() {
        return Memory.CURRENT.zwin.get(Memory.CURRENT.currentScreen).promptForInput();
    }

    public void setBuffered(boolean buffered) {
        if (!streams[3] && streams[1]) {
            Memory.CURRENT.zwin.get(Memory.CURRENT.currentScreen).setBuffered(buffered);
        }
    }

    public void setOutputStream(int number, int table, int width) {
        Log.i("Xyzzy", "Setting output stream:" + number + " :" + table + " :" + width);
        if (number == 0) {
            return;
        }
        streams[Math.abs(number)] = number > 0;
        if (number == 3) { // enable stream 3
            stream3position = table;
        } else if (number == -3) {//disable stream 3
            Memory.CURRENT.buffer.putShort(stream3position, stream3output.length());
            for (int i = 0, j = stream3output.length(); i < j; i++) {
                Memory.CURRENT.buffer.put(stream3position + 2 + i, stream3output.charAt(i));
            }
            //            Memory.CURRENT.buffer.put(stream3position + 2 + stream3output.length(), 0);
            stream3output.setLength(0);
        }
    }
}
