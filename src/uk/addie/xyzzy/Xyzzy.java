
package uk.addie.xyzzy;

import uk.addie.xyzzy.header.Header;
import uk.addie.xyzzy.opcodes.Opcode;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.zmachine.Decoder;
import uk.addie.xyzzy.zobjects.ZObject;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.util.Log;

public class Xyzzy implements Runnable {
    public static String story;

    @Override public void run() {
        Log.i("Xyzzy", "Starting background logic thread");
        Memory.CURRENT.storyPath = story;
        Header.reset();
        ZWindow.defaultColours();
        Opcode.RESTART.invoke(null);
        ZObject.enumerateObjects();
        Decoder.beginDecoding();
        Log.i("Xyzzy", "Finishing background logic thread");
    }
}
