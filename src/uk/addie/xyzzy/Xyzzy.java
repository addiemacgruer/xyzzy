
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
    private static String story;

    public Xyzzy(String story) {
        Xyzzy.story = story;
    }

    @Override public void run() {
        try {
            Log.i("Xyzzy", "Starting background logic thread");
            try {
                Memory.current().storyPath = story;
            } catch (final XyzzyException xe) {
                Log.e("Xyzzy", "Xyzzy.run:", xe);
                return;
            }
            Header.reset();
            Memory.current().zwin.clear();
            for (int i = 0; i < 8; i++) {
                Memory.current().zwin.put(i, new ZWindow(i));
            }
            ZWindow.defaultColours();
            Memory.current().currentScreen = 0;
            Opcode.RESTART.invoke(null);
            ZObject.enumerateObjects();
            Decoder.beginDecoding();
            Log.i("Xyzzy", "Finishing background logic thread");
        } catch (Exception e) {
            final ZWindow window0 = Memory.current().zwin.get(0);
            window0.append("Problem with story file:");
            window0.println();
            window0.append(e.toString());
            window0.flush();
        }
    }
}
