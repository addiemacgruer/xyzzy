
package uk.addie.xyzzy.zmachine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

public class ZStack<T> implements Serializable, Iterable<T> {
    private final T           nullValue;
    private static final long serialVersionUID = 1L;
    private final List<T>     list             = new ArrayList<T>();
    private T                 last             = null;
    private int               size             = 0;

    public ZStack(T nullValue) {
        this.nullValue = nullValue;
    }

    public void add(T value) {
        list.add(value);
        last = value;
        size++;
    }

    public void clear() {
        list.clear();
        last = null;
        size = 0;
    }

    public T get(int i) {
        if (i < 0 || i >= list.size()) {
            Log.w("Xyzzy", "Stack index out of range.");
            return nullValue;
        }
        return list.get(i);
    }

    @Override public Iterator<T> iterator() {
        return list.iterator();
    }

    public T peek() {
        if (last == null) {
            last = list.get(size - 1);
        }
        return last;
    }

    public T pop() {
        T rval = peek();
        last = null;
        list.remove(size - 1);
        size--;
        return rval;
    }

    public void push(T value) {
        last = value;
        list.add(value);
        size++;
    }

    public int size() {
        return size;
    }

    @Override public String toString() {
        StringBuilder rval = new StringBuilder();
        rval.append('[');
        boolean first = true;
        for (T val : list) {
            if (!first) {
                rval.append(',');
            }
            first = false;
            rval.append(val.toString());
        }
        rval.append(']');
        return rval.toString();
    }
}
