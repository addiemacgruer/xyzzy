
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
        for (int i = 0; i < opmap.length; i++) {
            opmap[i] = new Opcode[0x20];
        }
    }

    public static void adjustForVersion(int version) {
        switch (version) {
        case 1:
            opmap[1][0xf] = Opcode.NOT;
            opmap[0][0x9] = Opcode.POP;
            return;
        case 2:
            opmap[1][0xf] = Opcode.NOT;
            opmap[0][0x9] = Opcode.POP;
            return;
        case 3:
            opmap[1][0xf] = Opcode.NOT;
            opmap[0][0x9] = Opcode.POP;
            return;
        case 4:
            opmap[1][0xf] = Opcode.NOT;
            opmap[0][0x9] = Opcode.POP;
            return;
        case 5:
        case 6:
        case 7:
        case 8:
        default:
            return;
        }
    }

    public static void invoke(final int operands, final int hex, ZStack<Short> arguments) {
        final Opcode z = opmap[operands][hex];
        if (z == null) {
            Log.e("Xyzzy",
                    "Illegal opcode:" + operands + "," + hex + " @"
                            + Integer.toHexString(Memory.CURRENT.callStack.peek().programCounter()));
            Error.ILL_OPCODE.invoke();
            return;
        }
        if (Debug.opcodes) {
            Log.i("Xyzzy", z + " " + Decoder.arguments());
        }
        z.invoke(arguments);
    }

    public static void map(final Opcode z) {
        if (opmap[z.operands][z.hex] != null) {
            Log.w("Xyzzy", "Duplicate zcode:" + z + " (was " + opmap[z.operands][z.hex] + ")");
        }
        opmap[z.operands][z.hex] = z;
    }
}
