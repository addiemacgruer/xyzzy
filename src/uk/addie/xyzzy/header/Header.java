package uk.addie.xyzzy.header;

import uk.addie.xyzzy.state.FileBuffer;
import uk.addie.xyzzy.state.Memory;

public enum Header {
  ABBREVIATIONS(24), //
  ALPHABET(52), //
  CHECKSUM(28), //
  CONFIG(1, 1, true), //
  DEFAULT_BACKGROUND(44, 1, true), //
  DEFAULT_FOREGROUND(45, 1, true), //
  DICTIONARY(8), //
  DYNAMIC_SIZE(14), //
  EXTENSION_TABLE(54), //
  FILE_SIZE(26), //
  FLAGS(16, true), //
  FONT_HEIGHT(38, 1, true), //
  FONT_WIDTH(39, 1, true), //
  FUNCTIONS_OFFSET(40), //
  GLOBALS(12), //
  HX_MOUSE_X(1, 1), //
  HX_MOUSE_Y(2, 1), //
  HX_TABLE_SIZE(0, 1), //
  HX_UNICODE_TABLE(3, 1), //
  INTERPRETER_NUMBER(30, 1, true), //
  INTERPRETER_VERSION(31, 1, true), // this is the font width in V5
  LINE_WIDTH(48), // this is the font height in V5
  OBJECTS(10), //
  RELEASE(2), //
  RESIDENT_SIZE(4), //
  SCREEN_COLS(33, 1, true), //
  SCREEN_HEIGHT(36, true), //
  SCREEN_ROWS(32, 1, true), //
  SCREEN_WIDTH(34, true), //
  SERIAL(18, 6), //
  STANDARD_HIGH(50, 1, true), //
  STANDARD_LOW(51, 1, true), //
  START_PC(6), //
  STRINGS_OFFSET(42), //
  TERMINATING_KEYS(46, 1), //
  USER_NAME(56), //
  VERSION(0, 1);
  private Header(final int offset) {
    this.offset = offset;
    length = 2;
    dynamic = false;
  }

  private Header(final int offset, final boolean dynamic) {
    this.offset = offset;
    length = 2;
    this.dynamic = dynamic;
  }

  private Header(final int offset, final int length) {
    this.offset = offset;
    this.length = length;
    dynamic = false;
  }

  private Header(final int offset, final int length, final boolean dynamic) {
    this.offset = offset;
    this.length = length;
    this.dynamic = dynamic;
  }

  public final boolean dynamic;

  public final int length;

  public final int offset;

  private int value = -1;

  public void put(final int s) {
    if (!dynamic)
      throw new UnsupportedOperationException();
    switch (length) {
    case 1:
      Memory.current().buff().put(offset, (byte) s);
      break;
    case 2:
      Memory.current().buff().putShort(offset, (short) s);
      break;
    default:
      throw new UnsupportedOperationException(Integer.toString(s));
    }
  }

  public int value() {
    if (!dynamic && value != -1)
      return value;
    return value(Memory.current().buffer);
  }

  public int value(final FileBuffer fb) {
    if (!dynamic && value != -1)
      return value;
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

  public static void reset() {
    for (final Header h : values()) {
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
}
