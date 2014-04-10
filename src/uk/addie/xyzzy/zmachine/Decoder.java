
package uk.addie.xyzzy.zmachine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import uk.addie.xyzzy.error.XyzzyException;
import uk.addie.xyzzy.opcodes.OpMap;
import uk.addie.xyzzy.opcodes.Opcode;
import uk.addie.xyzzy.os.Debug;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.util.Bit;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.util.Log;

public class Decoder {
    private static boolean             finished  = false;
    private static final ZStack<Short> arguments = new ZStack<Short>((short) 0);
    private static int                 opcount   = 1;

    public static ZStack<Short> arguments() {
        return arguments;
    }

    public static void beginDecoding() {
        finished = false;
        do {
            final CallStack callStack = Memory.current().callStack.peek();
            if (Debug.stack) {
                Log.i("Xyzzy", callStack.toString());
            }
            if (Debug.opcodes) {
                Log.i("Xyzzy", opcount + ": " + Integer.toHexString(callStack.programCounter()) + ": ");
            }
            arguments.clear();
            final int opcode = callStack.getProgramByte();
            if (Debug.copcodes) {
                printCOpcode(opcode);
            }
            opcount++;
            try {
                interpretOpcode(opcode);
            } catch (XyzzyException xe) { //oops
                Memory.streams().append("\n\nFatal error in story file\n");
                Log.e("Xyzzy", "Interpreter:" + xe.toString());
                xe.printStackTrace();
                flushTraceToScreen0(xe);
                finished = true;
            } catch (Exception e) { // double oops
                Memory.streams().append("\n\nFatal error in interpreter\n");
                Log.e("Xyzzy", "Runtime:" + e.toString());
                e.printStackTrace();
                flushTraceToScreen0(e);
                finished = true;
            }
        } while (!finished);
    }

    private static void flushTraceToScreen0(Exception xe) {
        if (!finished) {
            Memory.streams().append(xe.toString() + "\n\n");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos);
            xe.printStackTrace(pw);
            pw.flush();
            String printLog = new String(baos.toByteArray());
            Memory.streams().append(printLog);
            ZWindow.printAllScreens();
        }
    }

    private static void intepretShortOpcode(final int opcode) {
        if ((opcode & 0x30) == 0x30) { // 0OP
            OpMap.invoke(0, opcode & 0xf, arguments);
        } else {// 1OP
            final int optype = (opcode & 0x30) >> 4;
            load_operand(optype);
            OpMap.invoke(1, opcode & 0xf, arguments);
        }
    }

    private static void interpretExtended() {
        final int opcode = Memory.current().callStack.peek().getProgramByte();
        loadOperands(Memory.current().callStack.peek().getProgramByte());
        OpMap.invoke(4, opcode, arguments);
    }

    private static void interpretLongOpcode(final int opcode) {
        if (Bit.bit6(opcode)) {
            load_operand(2);
        } else {
            load_operand(1);
        }
        if (Bit.bit5(opcode)) {
            load_operand(2);
        } else {
            load_operand(1);
        }
        OpMap.invoke(2, opcode & 0x1f, arguments);
    }

    private static void interpretOpcode(final int opcode) {
        if (Bit.bit7(opcode) && Bit.bit6(opcode)) { //var, 0b11xxxxxx
            interpretVarOpcode(opcode);
        } else if (opcode == 0xbe) { // extended
            interpretExtended();
        } else if (Bit.bit7(opcode)) { // short, 0b10xxxxxx
            intepretShortOpcode(opcode);
        } else { // long
            interpretLongOpcode(opcode);
        }
    }

    private static void interpretVarOpcode(final int opcode) {
        final CallStack callStack = Memory.current().callStack.peek();
        if (Bit.bit5(opcode) && ((opcode & 0x1f) == 12 || (opcode & 0x1f) == 26)) {
            final int vc1 = callStack.getProgramByte();
            final int vc2 = callStack.getProgramByte();
            loadOperands(vc1); // loads of operands
            loadOperands(vc2);
        } else {
            loadOperands(callStack.getProgramByte());
        }
        OpMap.invoke(Bit.bit5(opcode) ? 3 : 2, opcode & 0x1f, arguments);
    }

    public static boolean isShuttingDown() {
        return finished;
    }

    private static void load_operand(final int type) {
        int value;
        if (Debug.stores) {
            switch (type) {
            case 0:
                Log.i("Xyzzy", "..large:");
                break;
            case 1:
                Log.i("Xyzzy", "..small:");
                break;
            default:
                break;
            }
        }
        final CallStack callStack = Memory.current().callStack.peek();
        switch (type) {
        case 0: // large constant
            value = callStack.getProgramByte() << 8;
            value += callStack.getProgramByte();
            break;
        case 1: //small constant
            value = callStack.getProgramByte();
            break;
        case 2: // variable
            final int variable = callStack.getProgramByte();
            if (Debug.stores) {
                if (variable == 0) {
                    Log.i("Xyzzy", "..stack:");
                } else if (variable < 16) {
                    Log.i("Xyzzy", "..local:" + variable + ":");
                } else {
                    Log.i("Xyzzy", "..global:" + variable + ":");
                }
            }
            value = Opcode.readValue(variable) & 0xffff;
            break;
        case 3: // omitted
            return;
        default:
            Log.e("Xyzzy", "Illegal operand type");
            return;
        }
        if (Debug.stores) {
            Log.i("Xyzzy", "value: " + value);
        }
        arguments.add(Short.valueOf((short) value));
    }

    private static void loadOperands(final int varSpecByte) {
        load_operand((varSpecByte & 0xc0) >> 6);
        load_operand((varSpecByte & 0x30) >> 4);
        load_operand((varSpecByte & 0x0c) >> 2);
        load_operand((varSpecByte & 0x03) >> 0);
    }

    private static void printCOpcode(final int opcode) {
        Log.i("Xyzzy", opcount + " @" + Integer.toHexString(Memory.current().callStack.peek().programCounter() - 1)
                + ":(");
        if (Bit.bit7(opcode) && Bit.bit6(opcode)) { //var, 0b11xxxxxx
            Log.i("Xyzzy", "3," + Integer.toHexString(opcode & 0x3f) + "):");
        } else if (opcode == 0xbe) { // extended
            Log.i("Xyzzy", "extended)");
        } else if (Bit.bit7(opcode)) { // short, 0b10xxxxxx
            if ((opcode & 0x30) == 0x30) {
                Log.i("Xyzzy", "0," + Integer.toHexString(opcode & 0xf) + "):");
            } else {
                Log.i("Xyzzy", "1," + Integer.toHexString(opcode & 0xf) + "):");
            }
        } else { // long
            Log.i("Xyzzy", "2," + Integer.toHexString(opcode & 0x1f) + "):");
        }
    }

    public static void terminate() {
        finished = true;
    }
}
