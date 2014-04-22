
package uk.addie.xyzzy.opcodes;

import uk.addie.xyzzy.error.Error;
import uk.addie.xyzzy.state.Memory;
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

    public static void adjustForVersion(final int version) {
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

    public static void invoke(final int operands, final int hex, final ZStack<Short> arguments) {
        final Opcode z = opmap[operands][hex];
        if (z == null) {
            Log.e("Xyzzy",
                    "Illegal opcode:" + operands + "," + hex + " @"
                            + Integer.toHexString(Memory.current().callStack.peek().programCounter()));
            Error.ILL_OPCODE.invoke();
            return;
        }
        z.invoke(arguments);
    }

    static void map(final Opcode z) {
        if (opmap[z.operands][z.hex] != null) {
            Log.w("Xyzzy", "Duplicate zcode:" + z + " (was " + opmap[z.operands][z.hex] + ")");
        }
        opmap[z.operands][z.hex] = z;
    }
}
