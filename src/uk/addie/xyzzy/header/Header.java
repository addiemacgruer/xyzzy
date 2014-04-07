
package uk.addie.xyzzy.header;

import uk.addie.xyzzy.state.FileBuffer;
import uk.addie.xyzzy.state.Memory;
import android.util.Log;

public enum Header {
    VERSION(0, 1), //
    CONFIG(1, 1, true), //
    RELEASE(2), //
    RESIDENT_SIZE(4), //
    START_PC(6), //
    DICTIONARY(8), //
    OBJECTS(10), //
    GLOBALS(12), //
    DYNAMIC_SIZE(14), //
    FLAGS(16, true), //
    SERIAL(18, 6), //
    ABBREVIATIONS(24), //
    FILE_SIZE(26), //
    CHECKSUM(28), //
    INTERPRETER_NUMBER(30, 1, true), //
    INTERPRETER_VERSION(31, 1, true), //
    SCREEN_ROWS(32, 1, true), //
    SCREEN_COLS(33, 1, true), //
    SCREEN_WIDTH(34, true), //
    SCREEN_HEIGHT(36, true), //
    FONT_HEIGHT(38, 1, true), // this is the font width in V5
    FONT_WIDTH(39, 1, true), //  this is the font height in V5
    FUNCTIONS_OFFSET(40), //
    STRINGS_OFFSET(42), //
    DEFAULT_BACKGROUND(44, 1, true), //
    DEFAULT_FOREGROUND(45, 1, true), //
    TERMINATING_KEYS(46, 1), //
    LINE_WIDTH(48), //
    STANDARD_HIGH(50, 1, true), //
    STANDARD_LOW(51, 1, true), //
    ALPHABET(52), //
    EXTENSION_TABLE(54), //
    USER_NAME(56), //
    HX_TABLE_SIZE(0, 1), //
    HX_MOUSE_X(1, 1), //
    HX_MOUSE_Y(2, 1), //
    HX_UNICODE_TABLE(3, 1);
    public static void printHeader(final FileBuffer bb) {
        Log.i("Xyzzy", "Header information:");
        for (final Header h : values()) {
            Log.i("Xyzzy", h + ":" + h.value());
        }
    }

    public static void reset() {
        for (Header h : values()) {
            h.value = -1;
        }
    }

    public static String serial(final FileBuffer fileBuffer) {
        final StringBuffer serial = new StringBuffer();
        for (int i = SERIAL.offset; i < SERIAL.offset + SERIAL.length; i++) {
            serial.append((char) fileBuffer.get(i));
        }
        return serial.toString();
    }

    public final int     offset;
    public final int     length;
    public final boolean dynamic;
    private int          value = -1;

    private Header(final int offset) {
        this.offset = offset;
        length = 2;
        this.dynamic = false;
    }

    private Header(final int offset, boolean dynamic) {
        this.offset = offset;
        length = 2;
        this.dynamic = dynamic;
    }

    private Header(final int offset, final int length) {
        this.offset = offset;
        this.length = length;
        this.dynamic = false;
    }

    private Header(final int offset, final int length, boolean dynamic) {
        this.offset = offset;
        this.length = length;
        this.dynamic = dynamic;
    }

    public void put(final int s) {
        if (!dynamic) {
            throw new UnsupportedOperationException();
        }
        switch (length) {
        case 1:
            Memory.CURRENT.buff().put(offset, (byte) s);
            break;
        case 2:
            Memory.CURRENT.buff().putShort(offset, (short) s);
            break;
        default:
            throw new UnsupportedOperationException(Integer.toString(s));
        }
    }

    public int value() {
        if (!dynamic && value != -1) {
            return value;
        }
        return value(Memory.CURRENT.buffer);
    }

    public int value(FileBuffer fb) {
        if (!dynamic && value != -1) {
            return value;
        }
        int rval = 0;
        for (int i = offset; i < offset + length; i++) {
            rval *= 0x100;
            rval += fb.get(i);
        }
        if (!dynamic) {
            value = rval;
        }
        return rval;
    }
}
