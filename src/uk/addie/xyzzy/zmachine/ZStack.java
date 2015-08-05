package uk.addie.xyzzy.zmachine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ZStack<T> implements Serializable, Iterable<T> {
  public ZStack(final T nullValue) {
    this.nullValue = nullValue;
  }

  private T last = null;

  private final List<T> list = new ArrayList<T>();

  private final T nullValue;

  private int size = 0;

  public void add(final T value) {
    list.add(value);
    last = value;
    size++;
  }

  @Override public Iterator<T> iterator() {
    return list.iterator();
  }

  public T peek() {
    if (last == null && size > 0) {
      last = list.get(size - 1);
    }
    return last != null ? last : nullValue;
  }

  public T pop() {
    final T rval = peek();
    last = null;
    // if (size > 1) {
    list.remove(size - 1);
    // }
    size--;
    return rval;
  }

  @Override public String toString() {
    final StringBuilder rval = new StringBuilder();
    rval.append('[');
    boolean first = true;
    for (final T val : list) {
      if (!first) {
        rval.append(',');
      }
      first = false;
      rval.append(val.toString());
    }
    rval.append(']');
    return rval.toString();
  }

  void clear() {
    list.clear();
    last = null;
    size = 0;
  }

  void push(final T value) {
    last = value;
    list.add(value);
    size++;
  }

  int size() {
    return size;
  }

  private static final long serialVersionUID = 1L;
}
