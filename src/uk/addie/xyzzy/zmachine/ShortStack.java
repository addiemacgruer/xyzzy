package uk.addie.xyzzy.zmachine;

import java.io.Serializable;

public class ShortStack implements Serializable {
  private final short[] values = new short[8];

  private int size = 0;

  public short get(final int i) {
    return values[i];
  }

  public int size() {
    return size;
  }

  @Override public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i = 0; i < size; i++) {
      if (i != 0) {
        sb.append(',');
      }
      sb.append(values[i]);
    }
    sb.append(']');
    return sb.toString();
  }

  void add(final short value) {
    values[size++] = value;
  }

  void clear() {
    for (int i = 0; i < values.length; i++) {
      values[i] = 0;
    }
    size = 0;
  }

  private static final long serialVersionUID = 1L;
}
