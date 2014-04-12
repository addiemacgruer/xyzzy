
package uk.addie.xyzzy.opcodes;

import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.os.Debug;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.zmachine.Decoder;
import uk.addie.xyzzy.zmachine.ZStack;
import android.util.Log;

public class OpMap {
    private static final Opcode[][] opmap;
    static {
        opmap = new Opcode[5][];
        opmap[2] = new Opcode[0x20];
        opmap[1] = new Opcode[0x10];
        opmap[0] = new Opcode[0x10];
        opmap[3] = new Opcode[0x20];
        opmap[4] = new Opcode[0x20];
    }

    public static void adjustForVersion(int version) {
        if (version <= 4) {
            opmap[1][0xf] = Opcode.NOT;
            opmap[0][0x9] = Opcode.POP;
            opmap[0][0x5] = Opcode.SAVE;
            opmap[0][0x6] = Opcode.RESTORE;
        } else {
            opmap[1][0xf] = Opcode.CALL_1N;
            opmap[0][0x9] = Opcode.CATCH;
            opmap[0][0x5] = null;
            opmap[0][0x6] = null;
        }
    }

    public static void invoke(final int operands, final int hex, ZStack<Short> arguments) {
        final Opcode z = opmap[operands][hex];
        if (z == null) {
            Log.e("Xyzzy",
                    "Illegal opcode:" + operands + "," + hex + " @"
                            + Integer.toHexString(Memory.current().callStack.peek().programCounter()));
            Error.ILL_OPCODE.invoke();
            return;
        }
        if (Debug.opcodes) {
            Log.i("Xyzzy", z + " " + Decoder.arguments());
        }
        z.invoke(arguments);
    }

    public static void logAllOpcodes() {
        for (int j = 0; j < opmap.length; j++) {
            for (int i = 0; i < opmap[j].length; i++) {
                Opcode z = opmap[j][i];
                if (z != null) {
                    Log.d("Xyzzy", j + ":" + Integer.toHexString(i) + " " + z);
                } else {
                    Log.d("Xyzzy", j + ":" + Integer.toHexString(i) + " illegal");
                }
            }
        }
    }

    public static void map(final Opcode z) {
        if (opmap[z.operands][z.hex] != null) {
            Log.w("Xyzzy", "Duplicate zcode:" + z + " (was " + opmap[z.operands][z.hex] + ")");
        }
        opmap[z.operands][z.hex] = z;
    }
}
