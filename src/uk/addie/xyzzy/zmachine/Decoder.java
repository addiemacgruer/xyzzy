package uk.addie.xyzzy.zmachine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import uk.addie.xyzzy.error.XyzzyException;
import uk.addie.xyzzy.opcodes.OpMap;
import uk.addie.xyzzy.opcodes.Opcode;
import uk.addie.xyzzy.state.Memory;
import uk.addie.xyzzy.util.Bit;
import uk.addie.xyzzy.zobjects.ZWindow;
import android.util.Log;

public class Decoder {
  private static final ShortStack arguments = new ShortStack();

  private static boolean finished = false;

  private static int opcount = 0;

  public static void beginDecoding() {
    finished = false;
    do {
      final CallStack callStack = Memory.current().callStack.peek();
      arguments.clear();
      final int opcode = callStack.getProgramByte();
      opcount++;
      try {
        interpretOpcode(opcode);
      } catch (final XyzzyException xe) { // oops
        Memory.streams().append("\n\nFatal error in story file\n");
        Log.e("Xyzzy", "Interpreter:", xe);
        // xe.printStackTrace();
        flushTraceToScreen0(xe);
        finished = true;
      } catch (final Exception e) { // double oops
        Memory.streams().append("\n\nFatal error in interpreter\n");
        Log.e("Xyzzy", "Runtime:", e);
        // e.printStackTrace();
        flushTraceToScreen0(e);
        finished = true;
      }
    } while (!finished);
  }

  public static boolean isShuttingDown() {
    return finished;
  }

  public static int opcount() {
    return opcount;
  }

  public static void resetOpcount() {
    opcount = 0;
  }

  public static void terminate() {
    finished = true;
  }

  private static void flushTraceToScreen0(final Exception xe) {
    if (!finished) {
      Memory.streams().append(xe.toString() + "\n\n");
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final PrintWriter pw = new PrintWriter(baos);
      xe.printStackTrace(pw);
      pw.flush();
      final String printLog = new String(baos.toByteArray());
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
    if (Bit.bit7(opcode) && Bit.bit6(opcode)) { // var, 0b11xxxxxx
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

  private static void load_operand(final int type) {
    int value;
    final CallStack callStack = Memory.current().callStack.peek();
    switch (type) {
    case 0: // large constant
      value = callStack.getProgramByte() << 8;
      value += callStack.getProgramByte();
      break;
    case 1: // small constant
      value = callStack.getProgramByte();
      break;
    case 2: // variable
      final int variable = callStack.getProgramByte();
      value = Opcode.readValue(variable) & 0xffff;
      break;
    case 3: // omitted
      return;
    default:
      Log.e("Xyzzy", "Illegal operand type");
      return;
    }
    arguments.add(Short.valueOf((short) value));
  }

  private static void loadOperands(final int varSpecByte) {
    load_operand((varSpecByte & 0xc0) >> 6);
    load_operand((varSpecByte & 0x30) >> 4);
    load_operand((varSpecByte & 0x0c) >> 2);
    load_operand((varSpecByte & 0x03) >> 0);
  }
}
