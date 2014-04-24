
package uk.addie.xyzzy.zmachine;

import java.io.Serializable;

public class ShortStack implements Serializable {
    private static final long serialVersionUID = 1L;
    private final short[]     values           = new short[8];
    private int               size             = 0;

    public void add(short value) {
        values[size++] = value;
    }

    public void clear() {
        for (int i = 0; i < values.length; i++) {
            values[i] = 0;
        }
        size = 0;
    }

    public short get(int i) {
        return values[i];
    }

    public int size() {
        return size;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
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
}
