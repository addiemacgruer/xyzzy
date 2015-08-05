package uk.addie.xyzzy.util;

public class Utility {
  public static <T> int arrayOffsetOf(final T item, final T[] mis) {
    for (int i = 0; i < mis.length; ++i) {
      if (item == mis[i])
        return i;
    }
    return -1;
  }
}
