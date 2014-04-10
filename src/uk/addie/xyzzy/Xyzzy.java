
package uk.addie.xyzzy;

import uk.addie.xyzzy.error.XyzzyException;
import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.opcodes.Opcode;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.zmachine.Decoder;
import uk.addie.xyzzy.zobjects.ZObject;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.util.Log;

class Xyzzy implements Runnable {
    static String story;

    @Override public void run() {
        Log.i("Xyzzy", "Starting background logic thread");
        try {
            Memory.current().storyPath = story;
        } catch (XyzzyException xe) {
            return;
        }
        Header.reset();
        ZWindow.defaultColours();
        Opcode.RESTART.invoke(null);
        ZObject.enumerateObjects();
        Decoder.beginDecoding();
        Log.i("Xyzzy", "Finishing background logic thread");
    }
}
