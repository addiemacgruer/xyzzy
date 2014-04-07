
package uk.addie.xyzzy.os;

import uk.addie.xyzzy.state.Memory;
import android.util.Log;

public class OS {
    public static void os_fatal(final String s) {
        Log.e("Xyzzy", s);
        System.exit(1);
    }

    static void os_process_arguments(final String[] args) {
        for (final String s : args) {
            if (s.equals("-debug")) {
                Debug.opcodes = true;
                Debug.stores = true;
                Debug.stack = true;
                Debug.callstack = true;
                Debug.jumps = true;
                Debug.screen = true;
                continue;
            } else if (s.equals("-cdebug")) {
                Debug.copcodes = true;
                Debug.stores = true;
                continue;
            } else if (s.equals("-moves")) {
                Debug.moves = true;
            } else {
                Memory.CURRENT.storyPath = s;
            }
        }
    }

    public static long os_random_seed() {
        return System.currentTimeMillis();
    }
}
