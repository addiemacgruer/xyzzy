
package uk.addie.xyzzy;


public class Utility {
    public static <T> int arrayOffsetOf(T item, T[] mis) {
        for (int i = 0; i < mis.length; ++i) {
            if (item == mis[i]) {
                return i;
            }
        }
        return -1;
    }
}
